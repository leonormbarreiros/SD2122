package pt.ulisboa.tecnico.classes.professor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CancelEnrollmentRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CancelEnrollmentResponse;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.OpenEnrollmentsRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.OpenEnrollmentsResponse;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CloseEnrollmentsRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CloseEnrollmentsResponse;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.ListClassRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.ListClassResponse;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode.OK;

public class ClassServerFrontend {
    private static final String READ = "read";
    private static final String WRITE = "write";
    private static final boolean PRIMARY = true;
    private static final boolean SECONDARY = false;
    private List<String> primaryServers;
    private List<String> secondaryServers;
    private int nServers; // total number of servers
    private int nPrimaries; // total number of primary servers
    private int nSecondaries; // total number of secondary servers

    /**
     * Whenever I send a message, I want to record who that
     * message was sent to, and timestamp that access.
     */
    private ArrayList<String> accesses = new ArrayList<>();

    private String latestOperation = READ; // latest op performed

    private int clock = 0; // Lamport's logical clock

    private static ManagedChannel channel;
    private static ProfessorServiceGrpc.ProfessorServiceBlockingStub stub;
    private final boolean debug; // determines if debug option is activated

    /**
     * Constructor
     *
     * @param debug, determines if debug option is activated
     */
    public ClassServerFrontend(boolean debug, List<String> primaryServers, List<String> secondaryServers) {
        this.debug = debug;
        this.primaryServers = primaryServers;
        this.secondaryServers = secondaryServers;
        this.nPrimaries = primaryServers.size(); // should be 1
        this.nSecondaries = secondaryServers.size();
        this.nServers = this.nPrimaries + this.nSecondaries;
    }

    /**
     * Method to determine if the debug option is activate.
     *
     * @return the value of the debug variable
     */
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

    /**
     * Creates a channel to communicate with the ClassServer.
     */
    public void createChannel(String target) {
        channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    }

    /**
     * Creates a blocking stub to serve as interface for communicating with the server.
     */
    public void createBlockingStub() {
        stub = ProfessorServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Exits
     */
    public void terminate(){
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
     * When the professor sends a message to the server,
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
     * Functions that sends the OpenEnrollmentsRequest request with 3 tries if fail
     * @param request
     * @return response null if it fails after 3 times
     */
    public OpenEnrollmentsResponse attemptOpenEnrollment(OpenEnrollmentsRequest request){
        int tries = 0;
        String server = nextAccess();
        connectServer(server);

        List<String> usedServers = new ArrayList<>();
        usedServers.add(server);

        while (tries < getNServers() * 3) {
            try {
                tries++;
                addOperation(server, WRITE);
                debug("About to attempt openEnrollment on server: " + server);
                OpenEnrollmentsResponse response = stub.withDeadlineAfter(3000, TimeUnit.MILLISECONDS).openEnrollments(request);
                debug("Received openEnrollment response with status: " + response.getCode());
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
     * Sends the openEnrollments request and receives the openEnrollments response from server
     *
     * @param number, capacity for the class
     * @return the response code
     */
    public String openEnrollments(int number){
        // construct request message
        OpenEnrollmentsRequest request = OpenEnrollmentsRequest.newBuilder().setCapacity(number).build();

        // send request message and receive response
        OpenEnrollmentsResponse response = attemptOpenEnrollment(request);
        if (response == null) {
            terminate();
            return Stringify.format(ResponseCode.UNRECOGNIZED) + "\n";
        }

        // print response
        terminate();
        return Stringify.format(response.getCode()) + "\n";
    }

    /**
     * Functions that sends the CloseEnrollmentsRequest request with 3 tries if fail
     * @param request
     * @return response null if it fails after 3 times
     */
    public CloseEnrollmentsResponse attemptCloseEnrollment(CloseEnrollmentsRequest request){
        int tries = 0;
        String server = nextAccess();
        connectServer(server);

        List<String> usedServers = new ArrayList<>();
        usedServers.add(server);

        while (tries < getNServers() * 3) {
            try {
                tries++;
                addOperation(server, WRITE);
                debug("About to attempt closeEnrollment on server: " + server);
                CloseEnrollmentsResponse response = stub.withDeadlineAfter(3000, TimeUnit.MILLISECONDS).closeEnrollments(request);
                debug("Received closeEnrollment response with status: " + response.getCode());
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
     * Sends the closeEnrollments request and receives the closeEnrollments response from server
     *
     * @return the response code
     */
    public String closeEnrollments(){
        // construct request message
        CloseEnrollmentsRequest request = CloseEnrollmentsRequest.newBuilder().build();

        // send request message and receive response
        CloseEnrollmentsResponse response = attemptCloseEnrollment(request);
        if (response == null) {
            terminate();
            return Stringify.format(ResponseCode.UNRECOGNIZED) + "\n";
        }

        // print response
        terminate();
        return Stringify.format(response.getCode()) + "\n";
    }

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
     * Functions that sends the cancelEnrollment request with 3 tries if fail
     * @param request
     * @return response null if it fail after 3 times
     */
    public CancelEnrollmentResponse attemptCancelEnrollment(CancelEnrollmentRequest request){
        int tries = 0;
        String server = nextAccess();
        connectServer(server);

        List<String> usedServers = new ArrayList<>();
        usedServers.add(server);

        while (tries < getNServers() * 3) {
            try {
                tries++;
                addOperation(server, WRITE);
                debug("About to attempt cancelEnrollment on server: " + server);
                CancelEnrollmentResponse response = stub.withDeadlineAfter(3000, TimeUnit.MILLISECONDS).cancelEnrollment(request);
                debug("Received cancelEnrollment response with status: " + response.getCode());
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
     * Sends the cancelEnrollment request and receives the cancelEnrollment response from server
     * @return message to be printed to the user
     */
    public String cancelEnrollment(String student_id){
        // construct request message
        CancelEnrollmentRequest request = CancelEnrollmentRequest.newBuilder().setStudentId(student_id).build();

        // send request message and receive response
        CancelEnrollmentResponse response = attemptCancelEnrollment(request);
        if (response == null) {
            terminate();
            return Stringify.format(ResponseCode.UNRECOGNIZED) + "\n";
        }

        // print response
        terminate();
        return Stringify.format(response.getCode()) + "\n";
    }

    /* * * * * * * * * * * * * * * * * * AUXILIARIES * * * * * * * * * * * * * * * * * * * */

    /**
     * Auxiliary function to use when attempting requests.
     * It deals with the exception given as argument, and
     * depending on that, it can lead to the professor giving up
     * on the request, or trying again.
     * @param e the exception that was thrown
     * @param tries the number of tries the professor has done to
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
