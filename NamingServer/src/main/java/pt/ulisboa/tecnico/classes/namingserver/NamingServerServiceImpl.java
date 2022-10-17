package pt.ulisboa.tecnico.classes.namingserver;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import static pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode.OK;
import pt.ulisboa.tecnico.classes.contract.naming.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;
import pt.ulisboa.tecnico.classes.namingserver.exceptions.DuplicateServerException;
import pt.ulisboa.tecnico.classes.namingserver.exceptions.DuplicateServiceException;
import pt.ulisboa.tecnico.classes.namingserver.exceptions.ServiceNotFoundException;
import pt.ulisboa.tecnico.classes.namingserver.exceptions.UnknowQualifierException;
import pt.ulisboa.tecnico.classes.namingserver.exceptions.UnknownServerException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import static io.grpc.Status.INVALID_ARGUMENT;

public class NamingServerServiceImpl extends NamingServerServiceGrpc.NamingServerServiceImplBase{

	private final NamingServices namingServices = new NamingServices();

	private boolean debug;

	/**
	 * Construtor
	 * @param debug
	 */
	public NamingServerServiceImpl(boolean debug){
		ServiceEntry serviceEntry = new ServiceEntry("Turmas");
		namingServices.addService(serviceEntry);
		this.debug = debug;
	}

	/**
	 * @return true if the debug mode is active, otherwise false
	 */
	public boolean isDebug() { return debug; }

	/**
	 * Prints a message about the status of NamingServices
	 */
	private void debug_status(){
		if(isDebug()) System.err.println(namingServices.list());
	}

	/** Helper method to print debug messages. */
	private void debug(String debugMessage) {
		if (isDebug()) System.err.println(debugMessage);
	}


	@Override
	public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
		debug("Received a register request message.");

		if (Context.current().isCancelled()) {
			responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Time's up!").asRuntimeException());
			return;
		}

		try{
			synchronized (namingServices){
				namingServices.register(request.getServiceName(), request.getHostPort(), request.getQualifierList());
			}

			RegisterResponse response = RegisterResponse.newBuilder().setCode(OK).build();

			debug("About to send a register response message with code: " + response.getCode());

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
		catch(UnknowQualifierException e){
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid request.").asRuntimeException());
		}
		catch(DuplicateServiceException | DuplicateServerException e){
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}

		debug_status();
	}

	@Override
	public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
		debug("Received a delete request message.");

		try{
			synchronized (namingServices){
				namingServices.delete(request.getServiceName(), request.getHostPort());
			}

			DeleteResponse response = DeleteResponse.newBuilder().setCode(OK).build();

			debug("About to send a delete response message with code: " + response.getCode());

			responseObserver.onNext(response);
			responseObserver.onCompleted();


		}
		catch(UnknownServerException e){
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}

		debug_status();
	}

	@Override
	public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
		if (Context.current().isCancelled()) {
			responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Time's up!").asRuntimeException());
			return;
		}

		debug("Received a lookup request message for service: " + request.getService() +
				", and qualifiers: " + request.getQualifiersList().toString());

		/* given the name of the service */
		String serviceName = request.getService();

		/* and the qualifiers the client desires */
		List<String> qualifiers = request.getQualifiersList();

		try {
			/* return the servers matching that request */
			LookupResponse.Builder responseBuilder = LookupResponse.newBuilder();

			synchronized (namingServices) {
				ServiceEntry serviceEntry = namingServices.getService(serviceName);

				Set<ServerEntry> serverEntries = serviceEntry.getServerEntries().stream()
						.filter(serverEntry -> serverEntry.getQualifiers().containsAll(qualifiers))
						.collect(Collectors.toSet());

				serverEntries.forEach(serverEntry -> responseBuilder.addServers(serverEntry.getHostPort()));
			}

			LookupResponse response = responseBuilder.build();
			debug("About to send a lookup response containing " + response.getServersCount() + " addresses.");

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}

		catch (ServiceNotFoundException e) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}

}
