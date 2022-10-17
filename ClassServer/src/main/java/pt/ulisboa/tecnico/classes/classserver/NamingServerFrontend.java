package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode.OK;

public class NamingServerFrontend extends pt.ulisboa.tecnico.classes.NamingServerFrontend {
	private final String host = "localhost";
	private final int port = 5000;

	private final boolean debug;

	/**
	 * Constructor
	 * @param debug true if the debug mode is active, otherwise false
	 */
	public NamingServerFrontend(boolean debug) {
		super(debug);
		createChannel(host, port);
		createBlockingStub();
		this.debug = debug;
	}

	/**
	 * @return true if the debug mode is active, otherwise false
	 */
	public boolean isDebug(){
		return debug;
	}

	/** Helper method to print debug messages. */
	private void debug(String debugMessage) {
		if (isDebug()) System.err.println(debugMessage);
	}

	/**
	 * Prints a message about program state.
	 * Use it before send a register request.
	 * @param request
	 */
	public void debug_registerRequest(RegisterRequest request){
		if (isDebug()) {
			System.err.println("About to send register request with the follow arguments: ");
			System.err.println("Server name -> " + request.getServiceName());
			System.err.println("Host:Port -> " + request.getHostPort());
			System.err.println("Qualifiers -> " + request.getQualifierList());
		}
	}

	/**
	 * Prints a message about program state.
	 * Use it before send a delete request.
	 * @param request
	 */
	public void debug_deleteRequest(DeleteRequest request){
		if (isDebug()) {
			System.err.println("About to send delete request with the follow arguments: ");
			System.err.println("Server name -> " + request.getServiceName());
			System.err.println("Host:Port -> " + request.getHostPort());
		}
	}

	/**
	 * Functions that sends the register request with 3 tries if fail
	 * @param request
	 * @return response null if it fail after 3 times
	 */
	public RegisterResponse attemptRegister(RegisterRequest request){
		RegisterResponse response = null;
		for(int tries = 1; tries <= 3; tries++){
			try{
				// send request message and receive response
				response = getStub().withDeadlineAfter(3000, TimeUnit.MILLISECONDS).register(request);
				debug("Received register response with status: " + response.getCode());

				return response;
			}catch(StatusRuntimeException e){
				// timeout
				if(Status.DEADLINE_EXCEEDED.getCode() == e.getStatus().getCode()){
					System.err.println("Timeout. " + tries + " out of 3 tries completed.");
				}
				//Server unavailble
				else if(Status.UNAVAILABLE.getCode() == e.getStatus().getCode()){
					System.err.println("Server unabailable. "+ tries + " out of 3 tries completed.");
				}
				//other exceptions
				else{
					System.err.println("Caught exception with description: " + e.getStatus().getDescription());
				}
			}
		}
		return response;
	}

	/**
	 * Sends the register request and receives the register response from server
	 * Finish the program if the register request fails.
	 */
	public void register(String serviceName, String hostPort, List<String> qualifiers){
		// construct request message
		RegisterRequest request = RegisterRequest.newBuilder()
				.setServiceName(serviceName)
				.setHostPort(hostPort)
				.addAllQualifier(qualifiers).build();
		debug_registerRequest(request);

		// send request message and receive response
		RegisterResponse response = attemptRegister(request);

		if(response == null){
			System.err.println("Register Failed!");
			System.exit(1);
		}
	}

	/**
	 * Functions that sends the register request with 3 tries if fail
	 * @param request
	 * @return response null if it fail after 3 times
	 */
	public DeleteResponse attemptDelete(DeleteRequest request){
		DeleteResponse response = null;
		for(int tries = 1; tries <= 3; tries++){
			try{
				// send request message and receive response
				response = getStub().withDeadlineAfter(3000, TimeUnit.MILLISECONDS).delete(request);
				debug("Received delete response with status: " + response.getCode());

				return response;
			}catch(StatusRuntimeException e){
				// timeout
				if(Status.DEADLINE_EXCEEDED.getCode() == e.getStatus().getCode()){
					System.err.println("Timeout. " + tries + " out of 3 tries completed.");
				}
				//Server unavailble
				else if(Status.UNAVAILABLE.getCode() == e.getStatus().getCode()){
					System.err.println("Server unabailable. "+ tries + " out of 3 tries completed.");
				}
				//other exceptions
				else{
					System.err.println("Caught exception with description: " + e.getStatus().getDescription());
				}
			}
		}
		return response;
	}

	public void delete(String serviceName, String hostPort){

		// construct request message
		DeleteRequest request = DeleteRequest.newBuilder()
				.setServiceName(serviceName)
				.setHostPort(hostPort).build();
		debug_deleteRequest(request);

		// send request message and receive response
		DeleteResponse response = attemptDelete(request);

		if(response == null){
			System.err.println("Delete Failed!");
			System.exit(1);
		}
	}

	public void exit(){
		getChannel().shutdownNow();
	}
}
