package pt.ulisboa.tecnico.classes;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupResponse;
import pt.ulisboa.tecnico.classes.contract.naming.NamingServerServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode.UNRECOGNIZED;


public class NamingServerFrontend {

    private static ManagedChannel channel;
    private static NamingServerServiceGrpc.NamingServerServiceBlockingStub stub;
    private boolean _debug;

    public NamingServerFrontend(boolean debug) {
        _debug = debug;
    }

    public boolean isDebug() {
        return _debug;
    }

    public static ManagedChannel getChannel() {
        return channel;
    }

    public static NamingServerServiceGrpc.NamingServerServiceBlockingStub getStub() {
        return stub;
    }

    /**
     * Creates communication channel with the naming server.
     */
    public void createChannel(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    }

    /**
     * Creates a blocking stub to serve as interface for communicating with the naming server.
     */
    public void createBlockingStub() {
        stub = NamingServerServiceGrpc.newBlockingStub(this.channel);
    }

    /**
     * Ends the communication with the naming server.
     */
    public void terminate() {
        channel.shutdownNow();
    }

    /**
     * Performs a lookup request to the NamingServer, requiring a server to communicate with.
     * @param service the service we wish to be performed (eg. Turmas)
     * @param qualifiers what the server must be (eg. P means primary)
     * @return a list containing the addresses which match the description
     */
    public List<String> lookup(String service, List<String> qualifiers) {
        LookupRequest request = LookupRequest.newBuilder().setService(service).addAllQualifiers(qualifiers).build();
        if (isDebug()) {
            debug_lookupRequest(request);
        }

        int tries = 0;
        while (tries < 3) {
            try {
                tries++;
                LookupResponse response = stub.withDeadlineAfter(3000, TimeUnit.MILLISECONDS).lookup(request);
                if (isDebug()) {
                    debug_lookupResponse(response);
                }
                return response.getServersList();
            }
            catch (StatusRuntimeException e) {
                if (Status.DEADLINE_EXCEEDED.getCode() == e.getStatus().getCode()) {
                    System.err.println("Timeout: " + tries + "/3 tries completed.");
                }

                else if (Status.UNAVAILABLE.getCode() == e.getStatus().getCode()) {
                    System.err.println("Server unavailable: " + tries + "/3 tries completed.");
                }

                else {
                    System.err.println("Caught exception with description: " + e.getStatus().getDescription());
                    terminate();
                    return new ArrayList<>();
                }
            }
        }
        return new ArrayList<>();
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * Outputs a small description of the lookup request to be sent
     * @param request the before-mentioned request
     */
    public void debug_lookupRequest(LookupRequest request) {
        System.err.println("About to send lookup request for service: " + request.getService());
    }

    /**
     * Outputs a small description of the lookup response received
     * @param response the before-mentioned response
     */
    public void debug_lookupResponse(LookupResponse response) {
        System.err.println("Received lookup response containing " + response.getServersCount() + " servers.");
    }
}
