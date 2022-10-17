package pt.ulisboa.tecnico.classes.student;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollResponse;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassResponse;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClassServerFrontend {

    /**
     * List of available primary servers.
     * This list should only contain *1* element.
     */
    private List<String> primaryServers;

    /**
     * List of available secondary servers.
     * This list can contain *n* elements.
     */
    private List<String> secondaryServers;

    private int nServers; // total number of servers

    private int nPrimaries; // total number of primary servers

    private int nSecondaries; // total number of secondary servers

    private final boolean debug; // debug flag

    private final String READ = "read";

    private final String WRITE = "write";

    /**
     * Whenever I send a message, I want to record who that
     * message was sent to, and timestamp that access.
     */
    private ArrayList<String> accesses = new ArrayList<>();

    private String latestOperation = READ; // latest op performed

    private int clock = 0; // Lamport's logical clock

    private static ManagedChannel channel;

    private static StudentServiceGrpc.StudentServiceBlockingStub stub;

    public ClassServerFrontend(boolean debug, List<String> primaryServers, List<String> secondaryServers) {
        this.debug = debug;
        this.primaryServers = primaryServers;
        this.secondaryServers = secondaryServers;
        this.nPrimaries = primaryServers.size(); // should be 1
        this.nSecondaries = secondaryServers.size();
        this.nServers = this.nPrimaries + this.nSecondaries;
    }

    public boolean isDebug() { return debug; }

    public int getClock() { return clock; }

    public void setClock(int clock) { this.clock = clock; }

    public String getLatestOperation() { return this.latestOperation; }

    public void setLatestOperation(String op) { this.latestOperation = op; }

    public int getNServers() { return nServers; }

    public int getNPrimaries() { return nPrimaries; }

    public int getNSecondaries() { return nSecondaries; }

    /**
     * Helper method to print debug messages.
     */
    private void debug(String debugMessage) {
        if (isDebug())
            System.err.println(debugMessage);
    }

    /* * * * * * * * * * * * * * * CONNECTION ESTABLISHMENT * * * * * * * * * * * * * * * * */

    /**
     * Creates a channel to communicate with the entity
     * specified by target
     * @param target the server we want to communicate with
     */
    public void createChannel(String target) {
        channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    }

    /**
     * Creates a blocking stub to serve as interface for
     * communicating with the server.
     */
    public void createBlockingStub() {
        stub = StudentServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Ends the communication with the server.
     */
    public void terminate() {
        channel.shutdownNow();
    }

    /**
     * Enables communication with the server specified by the
     * parameter
     * @param target the server's address
     */
    private void connectServer(String target) {
        createChannel(target);
        createBlockingStub();
    }

    /* * * * * * * * * * * * * * * * * CLASSSERVER CHOICE * * * * * * * * * * * * * * * * * */

    /**
     * When the student sends a message to the server,
     * we save which server it was sent to, as well as
     * the type of operation it does.
     * @param server the server to whom we'll send the message
     */
    public void addOperation(String server, String op) {
        accesses.add(getClock(), server);
        setLatestOperation(op);
        setClock(getClock() + 1);
    }

    /**
     * Determines the next server to be contacted, from the
     * available ones.
     * @return the server's address
     */
    public String nextAccess() {
        // if i've just written, read my write
        if (getLatestOperation() == WRITE) {
            return accesses.get(getClock() - 1);
        }
        // if there's any server I haven't contacted, do it
        for (int i = 0; i < getNPrimaries(); i++) {
            if (!accesses.contains(primaryServers.get(i))) {
                return primaryServers.get(i);
            }
        }
        for (int i = 0; i < getNSecondaries(); i++) {
            if (!accesses.contains(secondaryServers.get(i))) {
                return secondaryServers.get(i);
            }
        }
        // else, divide between the servers
        int min = getClock();
        String server = "";
        for (int i = 0; i < getClock(); i++) {
            int count = 0;
            String serverAux = accesses.get(i);
            // count occurrences of serverAux
            for (int j = 0; j < getClock(); j++) {
                if (serverAux.equals(accesses.get(j)))
                    count++;
            }
            // if it has less, substitute
            if (count <= min) {
                min = count;
                server = serverAux;
            }
        }
        return server;
    }

    /**
     * If I've already tried to contact the servers in the
     * list, attempt to contact the next best one.
     * @param usedServers the servers I've already contacted
     * @return the next server to reach out to
     */
    public String chooseAnotherServer(List<String> usedServers) {
        int min = getClock();
        String server = "";

        // if there's any server I haven't contacted, do it
        for (int i = 0; i < getNPrimaries(); i++) {
            String serverAux = primaryServers.get(i);
            if (!accesses.contains(serverAux) && !usedServers.contains(serverAux)) {
                return serverAux;
            }
        }
        for (int i = 0; i < getNSecondaries(); i++) {
            String serverAux = secondaryServers.get(i);
            if (!accesses.contains(serverAux) && !usedServers.contains(serverAux)) {
                return serverAux;
            }
        }

        // find the next least used server
        for (int i = 0; i < getClock(); i++) {
            int count = 0;
            String serverAux = accesses.get(i);
            if (usedServers.contains(serverAux)) {
                continue;
            }
            else {
                // count occurrences of serverAux
                for (int j = 0; j < getClock(); j++) {
                    if (serverAux.equals(accesses.get(j)))
                        count++;
                }
                // if it has less, substitute
                if (count <= min) {
                    min = count;
                    server = serverAux;
                }
            }
        }
        return server;
    }

    /* * * * * * * * * * * * * * * * * MESSAGES /EVENTS * * * * * * * * * * * * * * * * * * */

    /**
     * In order to give strength to our program, and to deal
     * with some congestions in the server, the lisClass
     * request tries to connect with each server 3 times, and
     * will only fail if communication with all of them fails.
     * @param request the request message we want to send
     * @return either null (request not successful) or a valid
     * response (request successful)
     */
    private ListClassResponse attemptListClass(ListClassRequest request) {
        int tries = 0;
        String server = nextAccess();
        connectServer(server);

        List<String> usedServers = new ArrayList<>();
        usedServers.add(server);

        while (tries < getNServers() * 3) {
            try {
                tries++;
                addOperation(server, READ);
                debug("About to attempt listClass on server: " + server);
                ListClassResponse response = stub.withDeadlineAfter(3000, TimeUnit.MILLISECONDS).listClass(request);
                debug("Received listClass response with status: " + response.getCode());
                if (response.getCode() != ResponseCode.INACTIVE_SERVER) {
                    return response;
                }
                else if (tries == getNServers() * 3) {
                    return response;
                }
            }
            catch (StatusRuntimeException e) {
                if (dealWithException(e, tries) == false) {
                    return null;
                }
            }
            if ( tries % 3 == 0) {
                debug("Attempting request on another server.");
                terminate();
                server = chooseAnotherServer(usedServers);
                connectServer(server);
                usedServers.add(server);
            }
        }
        return null;
    }

    /**
     * Sends a listClass request message and waits for a
     * response containing the class's state.
     */
    public void listClass() {
        // construct request message
        ListClassRequest request = ListClassRequest.newBuilder().build();

        // send request message and receive response
        ListClassResponse response = attemptListClass(request);
        if (response == null) {
            System.out.println(Stringify.format(ResponseCode.UNRECOGNIZED) + "\n");
            terminate();
            return;
        }

        // print response
        ResponseCode status = response.getCode();
        if (status == ResponseCode.OK) {
            System.out.println(Stringify.format(response.getClassState()) + "\n");
        }
        terminate();
    }

    /**
     * In order to give strength to our program, and to deal
     * with some congestions in the server, the enroll
     * request tries to connect with each server 3 times, and
     * will only fail if communication with all of them fails.
     * @param request the request message we want to send
     * @return either null (request not successful) or a valid
     * response (request successful)
     */
    private EnrollResponse attemptEnroll(EnrollRequest request) {
        int tries = 0;
        String server = nextAccess();
        connectServer(server);

        List<String> usedServers = new ArrayList<>();
        usedServers.add(server);

        while (tries < getNServers() * 3) {
            try {
                tries++;
                addOperation(server, WRITE);
                debug("About to attempt enroll on server: " + server);
                EnrollResponse response = stub.withDeadlineAfter(3000, TimeUnit.MILLISECONDS).enroll(request);
                debug("Received enroll response with status: " + response.getCode());
                if (response.getCode() != ResponseCode.INACTIVE_SERVER) {
                    return response;
                }
                else if (tries == getNServers() * 3) {
                    return response;
                }
            }
            catch (StatusRuntimeException e) {
                if (dealWithException(e, tries) == false) {
                    return null;
                }
            }
            if (tries % 3 == 0) {
                debug("Attempting request on another server");
                terminate();
                server = chooseAnotherServer(usedServers);
                connectServer(server);
                usedServers.add(server);
            }
        }
        return null;
    }

    /**
     * Sends an enroll request message, containing the student
     * to be enrolled, and waits for a response containing the
     * status of that operation.
     */
    public void enroll(String studentId, String studentName) {

        // construct request message
        EnrollRequest request = EnrollRequest.newBuilder().setStudent(Student.newBuilder().setStudentId(studentId).setStudentName(studentName).build()).build();

        // send request message and receive response
        EnrollResponse response = attemptEnroll(request);
        if (response == null) {
            System.out.println(Stringify.format(ResponseCode.UNRECOGNIZED) + "\n");
            terminate();
            return;
        }

        // print response
        System.out.println(Stringify.format(response.getCode()) + "\n");
        terminate();
    }

    /* * * * * * * * * * * * * * * * * * AUXILIARIES * * * * * * * * * * * * * * * * * * * */

    /**
     * Auxiliary function to use when attempting requests.
     * It deals with the exception given as argument, and
     * depending on that, it can lead to the student giving up
     * on the request, or trying again.
     * @param e the exception that was thrown
     * @param tries the number of tries the student has done to
     *              perform that request
     * @return true if it can try again, false otherwise
     */
    public Boolean dealWithException(StatusRuntimeException e, int tries) {
        if (Status.DEADLINE_EXCEEDED.getCode() == e.getStatus().getCode()) {
            debug("Timeout: " + tries % 3 + "/3 tries completed.");
            return true;
        }
        else if (Status.UNAVAILABLE.getCode() == e.getStatus().getCode()) {
            debug("Server unavailable: " + tries + "/3 tries completed.");
            return true;
        }
        else {
            debug("Caught exception with description: " + e.getStatus().getDescription());
            return false;
        }
    }
}