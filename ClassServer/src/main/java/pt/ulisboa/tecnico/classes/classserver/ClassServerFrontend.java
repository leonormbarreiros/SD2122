package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.Validate;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.*;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ClassServerFrontend {
	private static ClassServerFrontend instance = null;
	private static final boolean TIMER = true;
	private static final boolean COMMAND = false;

	private final  String service = "Turmas";

	private boolean activeGossip = true;

	private ClassDomain class_ = ClassDomain.getInstance();

	private final boolean debug;

	private final Validate _validate = new Validate();

	private ManagedChannel channel = null;

	private ClassServerServiceGrpc.ClassServerServiceBlockingStub stub = null;

	private String target = "";

	private Map<Integer, String> _writes = new TreeMap<>(); // writes not yet propagated

	private int _clock = 0; // Lamport clock to help with replication

	public void setClock(int clock) { this._clock = clock; }

	public int getClock() { return _clock; }

	public void clearWrites() { _writes.clear(); }

	public Map<Integer, String> get_writes() { return _writes; }

	public void addWrite(Integer time, String write){ _writes.put(time, write); }

	/**
	 * @return true if the debug mode is active, otherwise false
	 */
	public boolean isDebug() {
		return debug;
	}

	public void setActiveGossip(boolean activeGossip) {
		this.activeGossip = activeGossip;
	}

	public boolean isActiveGossip() {
		return activeGossip;
	}

	/**
	 * Helper method to print debug messages.
	 * Constructor
	 */
	private void debug(String debugMessage) {
		if (isDebug())
			System.err.println(debugMessage);
	}

	private ClassServerFrontend(String target, boolean debug) {
		channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
		stub = ClassServerServiceGrpc.newBlockingStub(channel);
		this.target = target;
		this.debug = debug;
	}

  	public static ClassServerFrontend getInstance(String target, boolean debug) {
		if (instance == null) {
		  instance = new ClassServerFrontend(target, debug);
		}
		return instance;
	}


	/**
	 * Prints a message about program state.
	 * Use it after receive a propagateState response.
	 * @param response
	 */
	private void debug_propagateStateResponse(PropagateStateResponse response) {
		System.err.printf("Received propagateState response with status: ");
		System.err.println(response.getCode());
	}

	public static ClassServerFrontend getInstanceWithoutArgs() {
		return instance;
	}

	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

	/**
	 * Constructs and returns a ClassState message, containing
	 * the class's current characteristics.
	 * @return the built message
	 */
	public ClassState buildClassState() {
		ClassState.Builder classStateBuilder = ClassState.newBuilder();
		synchronized (class_) {
			Student.Builder studentBuilder = Student.newBuilder();
			classStateBuilder.setCapacity(class_.getCapacity());
			classStateBuilder.setOpenEnrollments(class_.isOpen());
			class_.getStudentsDiscarded().forEach(
					(id, name) ->
							classStateBuilder.addDiscarded(
									studentBuilder.setStudentId(id).setStudentName(name).build()));
			class_.getStudentsEnrolled().forEach(
					(id, name) ->
							classStateBuilder.addEnrolled(
									studentBuilder.setStudentId(id).setStudentName(name).build()));
		}
		return classStateBuilder.build();
	}

	/**
	 * Constructs and returns a PropagateStateRequest message,
	 * which contains the class state, as well as the writes
	 * performed since the last propagate.
	 * @return the built message
	 */
	public PropagateStateRequest buildPropagateStateRequest() {
		PropagateStateRequest.Builder requestBuilder = PropagateStateRequest.newBuilder();

		ClassState classState = buildClassState();
		requestBuilder.setClassState(classState);

		Set<Integer> times = _writes.keySet();
		for (Integer time : times) {
			String write = _writes.get(time);
			WriteRequest writeRequest = WriteRequest.newBuilder().setWrite(write).setClock(time).build();
			requestBuilder.addWrites(writeRequest);
		}
		return requestBuilder.build();
	}

	/**
	 * In order to strengthen our program, and to antecipate
	 * congestions in the other server, we try to connect with
	 * it 3 times. We will only fail after those attempts.
	 * @param request the request message we want to send
	 * @return null if we not successful, response otherwise
	 */
	public PropagateStateResponse attemptPropagateState(PropagateStateRequest request){
		PropagateStateResponse response = null;
		// TODO more than one other server
		for(int tries = 1; tries <= 3; tries++){
			try{
				//send request message and receive response
				debug("About to send a propagateState request. "); //TODO which server
				response = stub.withDeadlineAfter(3000, TimeUnit.MILLISECONDS).propagateState(request);
				debug("Received propagateState response with status: " + response.getCode());

				// if the server is inactive, give another chance
				if (response.getCode().equals(ResponseCode.INACTIVE_SERVER)){
					debug("Inactive Server. " + tries + " out of 3 tries completed.");
					continue;
				}
				// successful request
				else{
					break;
				}
			}
			catch(StatusRuntimeException e){
				// timeout
				if(Status.DEADLINE_EXCEEDED.getCode() == e.getStatus().getCode()){
					debug("Timeout. " + tries + " out of 3 tries completed.");
				}
				//Server unavailble
				else if(Status.UNAVAILABLE.getCode() == e.getStatus().getCode()){
					debug("Server unabailable. "+ tries + " out of 3 tries completed.");
				}
				//other exceptions
				else{
					debug("Caught exception with description: " + e.getStatus().getDescription());
				}
			}
		}
		return response;
	}

	/**
	 * Sends the propagateState request and receives the
	 * propagateState response from server
	 * @param flag TIMER or COMMAND
	 * @return True if successful
	 *
	 */
	public boolean propagateState(boolean flag){
		if(!isActiveGossip() && flag == TIMER){
			return false;
		}
		debug("########## Start Propagate ##########");
		//build request
		PropagateStateRequest request = buildPropagateStateRequest();
		debug("About to send propagateState request");

		PropagateStateResponse response = attemptPropagateState(request);
		if(response == null){
			debug("PropagateState failed.");
		}
		else{
			System.out.println(Stringify.format(response.getCode()));
		}

		ClassState classState = response.getClassState();
		validateClassState(response);
		synchronized (class_) {
			updateState(response);
		}
		clearWrites();
		debug("########## Finish Propagate ##########");

		return true;
	}


	/**
	 * Changes the class's state to the one given by the response.
	 * @param response the response containing the class's state.
	 */
	public void updateState(PropagateStateResponse response){
		class_.setOpen(response.getClassState().getOpenEnrollments());
		class_.setCapacity(response.getClassState().getCapacity());

		ConcurrentHashMap<String,String> enrolledStudents = new ConcurrentHashMap<>();
		response.getClassState().getEnrolledList()
				.forEach(student -> enrolledStudents.put(student.getStudentId(), student.getStudentName()));
		class_.setStudentsEnrolled(enrolledStudents);

		ConcurrentHashMap<String,String> discardedStudents = new ConcurrentHashMap<>();
		response.getClassState().getDiscardedList()
				.forEach(student -> discardedStudents.put(student.getStudentId(), student.getStudentName()));
		class_.setStudentsDiscarded(discardedStudents);

		debug("########## ClassState ##########");
		debug(class_.toString());
		debug("################################");
	}


	/**
	 * Ensures the response containing a class state is valid.
	 * This guaranties we won't have odd class states.
	 * @param response the message containing a class state
	 */
	public void validateClassState(PropagateStateResponse response){
		_validate.validate_capacity(response.getClassState().getCapacity());

		response.getClassState().getDiscardedList()
				.forEach(student -> _validate.validate_enrollment(student.getStudentId(), student.getStudentName()));

		response.getClassState().getEnrolledList()
				.forEach(student -> _validate.validate_enrollment(student.getStudentId(), student.getStudentName()));
	}

}
