package pt.ulisboa.tecnico.classes.admin;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DumpRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DumpResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateGossipRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateGossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateGossipRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateGossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.GossipRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.GossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.classes.Stringify;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode.OK;
import static pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode.UNRECOGNIZED;

public class ClassServerFrontend {
	private final boolean debug; // debug flag

	/** List of available primary servers. This list should only contain *1* element. */
	private List<String> primaryServers;

	/** List of available secondary servers. This list can contain *n* elements. */
	private List<String> secondaryServers;

	private ManagedChannel channel;
	private AdminServiceGrpc.AdminServiceBlockingStub stub;

	/**
	 * Constructor
	 *
	 * @param debug true if the debug mode is active, otherwise false
	 */
	public ClassServerFrontend(boolean debug, List<String> primaryServers, List<String> secondaryServers) {
		this.debug = debug;
		this.primaryServers = primaryServers;
		this.secondaryServers = secondaryServers;
	}

	/**
	 * @return true if the debug mode is active, otherwise false
	 */
	public boolean isDebug() {
		return debug;
	}

	/** Helper method to print debug messages. */
	private void debug(String debugMessage) {
		if (isDebug()) System.err.println(debugMessage);
	}

	/* * * * * * * * * * * * * * * CONNECTION ESTABLISHMENT * * * * * * * * * * * * * * * * */

	/** Creates a channel to communicate with the ClassServer. */
	public void createChannel(String target) {
		channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
	}

	/** Creates a blocking stub to serve as interface for communicating with the server. */
	public void createBlockingStub() {
		stub = AdminServiceGrpc.newBlockingStub(channel);
	}

	public void terminate() {
		channel.shutdownNow();
	}

	/**
	 * Choose which Server to connect depending on the requestType and connect it
	 *
	 * @param requestType
	 */
	public void chooseServer(String requestType) {
		String target = null;

		switch (requestType) {
			case "P":
				target = primaryServers.get(0);
				break;
			case "S":
				target = secondaryServers.get(0);
				break;
		}
		createChannel(target);
		createBlockingStub();
	}

	/* * * * * * * * * * * * * * * * * MESSAGES /EVENTS * * * * * * * * * * * * * * * * * * */

	/**
	 * Sends the activate request and receives the activate response from server
	 *
	 * @return message to be printed to the user
	 */
	public String activate(String qualifier) {
		chooseServer(qualifier);

		// construct request message
		ActivateRequest request = ActivateRequest.newBuilder().build();
		debug("About to send a activate request message.");

		int tries = 0;
		while (tries < 3) {
			try {
				tries++;
				ActivateResponse response =
						stub.withDeadlineAfter(3000, TimeUnit.MILLISECONDS).activate(request);

				debug("Received a activate response with code: " + response.getCode());

				terminate();
				return Stringify.format(response.getCode()) + "\n";
			} catch (StatusRuntimeException e) {
				dealWithException(e, tries);
			}
		}
		terminate();
		return Stringify.format(UNRECOGNIZED) + "\n";
	}

	public String deactivate(String qualifier) {
		chooseServer(qualifier);

		// construct request message
		DeactivateRequest request = DeactivateRequest.newBuilder().build();
		debug("About to send a deactivate request message.");

		int tries = 0;
		while (tries < 3) {
			try {
				tries++;
				DeactivateResponse response =
						stub.withDeadlineAfter(3000, TimeUnit.MILLISECONDS).deactivate(request);

				debug("Received a deactivate response with code: " + response.getCode());
				terminate();
				return Stringify.format(response.getCode()) + "\n";
			} catch (StatusRuntimeException e) {
				dealWithException(e, tries);
			}
		}
		terminate();
		return Stringify.format(UNRECOGNIZED) + "\n";
	}

	/**
	 * Functions that sends the dump request with 3 tries if fail
	 *
	 * @param request
	 * @return response null if it fail after 3 times
	 */
	public DumpResponse attemptDump(DumpRequest request) {
		DumpResponse response = null;
		for (int tries = 1; tries <= 3; tries++) {
			try {
				// send request message and receive response
				response = stub.withDeadlineAfter(3000, TimeUnit.MILLISECONDS).dump(request);
				debug("Received dump response with status: " + response.getCode());

				// check the response
				if (response.getCode().equals(ClassesDefinitions.ResponseCode.INACTIVE_SERVER)) {
					System.err.println("Inactive Server. " + tries + " out of 3 tries completed.");
					continue;
				} else {
					break;
				}
			} catch (StatusRuntimeException e) {
				dealWithException(e, tries);
			}
		}
		return response;
	}

	/**
	 * Sends the dump request and receives the dump response from server
	 *
	 * @return message to be printed to the user
	 */
	public String dump(String qualifier) {
		chooseServer(qualifier);

		// construct request message
		DumpRequest request = DumpRequest.newBuilder().build();
		debug("About to send dump request.");

		DumpResponse response = attemptDump(request);

		// format response
		if (response == null) {
			terminate();
			return Stringify.format(ClassesDefinitions.ResponseCode.UNRECOGNIZED) + "\n";
		} else if (response.getCode() != OK) {
			terminate();
			return Stringify.format(response.getCode()) + "\n";
		}

		terminate();
		return Stringify.format(response.getClassState()) + "\n";
	}

	/**
	 * Functions that sends the activate_gossip request with 3 tries if fail
	 *
	 * @param request
	 * @return response null if it fail after 3 times
	 */
	public ActivateGossipResponse attemptActivateGossip(ActivateGossipRequest request) {
		ActivateGossipResponse response = null;
		for (int tries = 1; tries <= 3; tries++) {
			try {
				// send request message and receive response
				response = stub.withDeadlineAfter(3000, TimeUnit.MILLISECONDS).activateGossip(request);
				debug("Received activate_gossip response with status: " + response.getCode());

				// check the response
				if (response.getCode().equals(ClassesDefinitions.ResponseCode.INACTIVE_SERVER)) {
					System.err.println("Inactive Server. " + tries + " out of 3 tries completed.");
					continue;
				} else {
					break;
				}
			} catch (StatusRuntimeException e) {
				dealWithException(e, tries);
			}
		}
		return response;
	}


	/**
	 * Sends the activate_gossip request and receives the activate_gossip response from server
	 *
	 * @return message to be printed to the user
	 */
	public String activate_gossip(String qualifier){
		chooseServer(qualifier);

		ActivateGossipRequest request = ActivateGossipRequest.newBuilder().build();
		debug("About to send activate_gossip request.");

		ActivateGossipResponse response = attemptActivateGossip(request);

		// format response
		if (response == null) {
			terminate();
			return Stringify.format(ClassesDefinitions.ResponseCode.UNRECOGNIZED) + "\n";
		}
		terminate();
		return Stringify.format(response.getCode()) + "\n";
	}

	/**
	 * Functions that sends the deactivate_gossip request with 3 tries if fail
	 *
	 * @param request
	 * @return response null if it fail after 3 times
	 */
	public DeactivateGossipResponse attemptDeactivateGossip(DeactivateGossipRequest request) {
		DeactivateGossipResponse response = null;
		for (int tries = 1; tries <= 3; tries++) {
			try {
				// send request message and receive response
				response = stub.withDeadlineAfter(3000, TimeUnit.MILLISECONDS).deactivateGossip(request);
				debug("Received deactivate_gossip response with status: " + response.getCode());

				// check the response
				if (response.getCode().equals(ClassesDefinitions.ResponseCode.INACTIVE_SERVER)) {
					System.err.println("Inactive Server. " + tries + " out of 3 tries completed.");
					continue;
				} else {
					break;
				}
			} catch (StatusRuntimeException e) {
				dealWithException(e, tries);
			}
		}
		return response;
	}


	/**
	 * Sends the deactivate_gossip request and receives the deactivate_gossip response from server
	 *
	 * @return message to be printed to the user
	 */
	public String deactivate_gossip(String qualifier){
		chooseServer(qualifier);

		DeactivateGossipRequest request = DeactivateGossipRequest.newBuilder().build();
		debug("About to send deactivate_gossip request.");

		DeactivateGossipResponse response = attemptDeactivateGossip(request);

		// format response
		if (response == null) {
			terminate();
			return Stringify.format(ClassesDefinitions.ResponseCode.UNRECOGNIZED) + "\n";
		}
		terminate();
		return Stringify.format(response.getCode()) + "\n";
	}

	/**
	 * Functions that sends the gossip request with 3 tries if fail
	 *
	 * @param request
	 * @return response null if it fail after 3 times
	 */
	public GossipResponse attemptGossip(GossipRequest request) {
		GossipResponse response = null;
		for (int tries = 1; tries <= 3; tries++) {
			try {
				// send request message and receive response
				response = stub.withDeadlineAfter(3000, TimeUnit.MILLISECONDS).gossip(request);
				debug("Received gossip response with status: " + response.getCode());

				// check the response
				if (response.getCode().equals(ClassesDefinitions.ResponseCode.INACTIVE_SERVER)) {
					System.err.println("Inactive Server. " + tries + " out of 3 tries completed.");
					continue;
				} else {
					break;
				}
			} catch (StatusRuntimeException e) {
				dealWithException(e, tries);
			}
		}
		return response;
	}


	/**
	 * Sends the gossip request and receives the gossip response from server
	 *
	 * @return message to be printed to the user
	 */
	public String gossip(String qualifier){
		chooseServer(qualifier);

		GossipRequest request = GossipRequest.newBuilder().build();
		debug("About to send gossip request.");

		GossipResponse response = attemptGossip(request);

		// format response
		if (response == null) {
			terminate();
			return Stringify.format(ClassesDefinitions.ResponseCode.UNRECOGNIZED) + "\n";
		}
		else if(response.getCode().equals(ClassesDefinitions.ResponseCode.WRITING_NOT_SUPPORTED)){
			terminate();
			return "Gossip is inactive\n";
		}
		terminate();
		return Stringify.format(response.getCode()) + "\n";
	}


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
	public void dealWithException(StatusRuntimeException e, int tries) {
		if (Status.DEADLINE_EXCEEDED.getCode() == e.getStatus().getCode()) {
			debug("Timeout. " + tries + " out of 3 tries completed.");
		}
		else if (Status.UNAVAILABLE.getCode() == e.getStatus().getCode()) {
			debug("Server unabailable. " + tries + " out of 3 tries completed.");
		}
		else {
			debug("Caught exception with description: " + e.getStatus().getDescription());
		}
	}

}
