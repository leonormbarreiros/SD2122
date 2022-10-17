package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.Validate;
import pt.ulisboa.tecnico.classes.classserver.exceptions.*;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.*;
import pt.ulisboa.tecnico.classes.exceptions.InvalidCapacityException;
import pt.ulisboa.tecnico.classes.exceptions.InvalidStudentException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.grpc.Status.INVALID_ARGUMENT;
import static pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode.OK;
import static pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode.INACTIVE_SERVER;


public class ClassServerServiceImpl extends ClassServerServiceGrpc.ClassServerServiceImplBase{
	private ClassDomain class_ = ClassDomain.getInstance();

	private boolean debug;

	private String type;

	private final Debug _debugger = new Debug();

	private final Validate _validate = new Validate();

	private ClassServerFrontend classServerFrontend = ClassServerFrontend.getInstanceWithoutArgs();

	public ClassServerServiceImpl(boolean debug, String type) {
		this.debug = debug;
		this.type = type;
	}

	public boolean isDebug() {
		return debug;
	}

	/**
	 * Helper method to print debug messages.
	 */
	private void debug(String debugMessage) {
		if (isDebug())
			System.err.println(debugMessage);
	}

	private String getType() { return type; }

	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

	/**
	 * Implements the rpc propagateState, from the service
	 * ClassServerService, defined in the Contract, by sending
	 * the class's state.
	 *
	 * @param request the message received
	 * @param responseObserver special interface for the server to call with its response
	 */
	@Override
	public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver){
		debug("Received a propagateState request message.");
		if (Context.current().isCancelled()) {
			responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Time's up!").asRuntimeException());
			return;
		}
		try{
			ClassState classState;
			synchronized (class_){
				if(!class_.isActive()){
					throw new InactiveServerException();
				}

				//validate arguments
				validateClassState(request);

				//updateState

				List<String> canceled = unifyState(request);
				for (String student : canceled) {
					try {
						class_.addNewDiscarded(student.split(":")[0], student.split(":")[1]);
					}
					catch (DuplicateCancellationException e) {
						debug("This shouldn't be happening.");
					}
				}
				classState = buildClassState();
			}

			classServerFrontend.clearWrites();

			//build response
			PropagateStateResponse.Builder responseBuilder = PropagateStateResponse.newBuilder();
			responseBuilder.setCode(OK);
			responseBuilder.setClassState(classState);
			PropagateStateResponse response = responseBuilder.build();

			debug("About to send a propagateState response message with code: " + response.getCode());
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (InactiveServerException e) {
			PropagateStateResponse response = PropagateStateResponse.newBuilder().setCode(INACTIVE_SERVER).build();
			debug("About to send a propagateState response message with code: " + response.getCode());
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (InvalidCapacityException e) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (InvalidStudentException e) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

	public void debug_ClassState(String classState){
		if(isDebug()){
			System.err.println("########## ClassState ##########");
			System.err.println(classState);
			System.err.println("################################");
		}
	}

	/**
	 * Changes the class's state to the one given by the request.
	 * @param request the request containing the class's state.
	 */
	public void updateState(PropagateStateRequest request){
		class_.setOpen(request.getClassState().getOpenEnrollments());
		class_.setCapacity(request.getClassState().getCapacity());

		ConcurrentHashMap<String,String> enrolledStudents = new ConcurrentHashMap<>();
		request.getClassState().getEnrolledList()
				.forEach(student -> enrolledStudents.put(student.getStudentId(), student.getStudentName()));
		class_.setStudentsEnrolled(enrolledStudents);

		ConcurrentHashMap<String,String> discardedStudents = new ConcurrentHashMap<>();
		request.getClassState().getDiscardedList()
				.forEach(student -> discardedStudents.put(student.getStudentId(), student.getStudentName()));
		class_.setStudentsDiscarded(discardedStudents);

		debug("########## ClassState ##########");
		debug(class_.toString());
		debug("################################");
	}

	/**
	 * Updates the class's state, by adding the (possible)
	 * enrollments made. If the request contains an open or
	 * close enrollments message, applies it directly.
	 * Returns a list of the refused enrollments.
	 * This function returns the same output whether I am the
	 * primary or the secondary servers, therefore, the replicas
	 * become consistent.
	 * @param request the message containing the changes the
	 *                other server applied
	 * @return the enrollments which were cancelled for
	 * overflowing the class
	 */
	public List<String> unifyState(PropagateStateRequest request) {
		List<String> cancelled = new ArrayList<>();
		List<String> toAdd = new ArrayList<>();

		// number of writes arithmetic
		int nRecWrites = request.getWritesCount();
		int nMyWrites = classServerFrontend.get_writes().size();
		int nMin = Math.min(nRecWrites, nMyWrites);

		// clock arithmetic
		int myClockMin = classServerFrontend.getClock() - nMyWrites;
		int recClockMin = getMinClock(request);

		/**
		 * When I receive a propagateState message, the writes
		 * are concurrent. This is because we clear them on
		 * every rpc, ensuring everything that "happened before"
		 * is secure.
		 * Therefore, I need to order the operations. *Bayou*
		 */

		/* 1. If the request has an "open", copy the full
			  class state in the request.
		 */
		for (int i = 0; i < nRecWrites; i++) {
			if (request.getWrites(i).getWrite().split(":")[0].equals("open")) {
				updateState(request);
				return cancelled;
			}
		}

		/* 2. Join all writes. If we find a "close" operation,
			  no other writes for that server will be added.
			  [The problem of close->open is mitigated in 1]
		*/
		boolean toClose = false;
		boolean iClosed = false;
		int n = 0;
		for (int i = 0; i < nMin; i++) {
			String myWrite = classServerFrontend.get_writes().get(i + myClockMin);
			String otWrite = getWriteGivenTime(request, i + recClockMin);

			if (myWrite.split(":")[0].equals("close")) {
				toClose = true;
				iClosed = true;
			}
			if (otWrite.split(":")[0].equals("close")) {
				toClose = true;
			}

			if (!toClose) {
				// this ensures the final states will be the same
				if (getType().equals("P")) {
					toAdd.add(n, myWrite);
					n++;
					toAdd.add(n, otWrite);
					n++;
				}
				else {
					toAdd.add(n, otWrite);
					n++;
					toAdd.add(n, myWrite);
					n++;
				}
			}
			// concurrent calls -> the one who didn't close can
			// add its writes (if there's capacity)
			else if (iClosed) {
				toAdd.add(n, otWrite);
				n++;
			}
			else {
				toAdd.add(n, myWrite);
				n++;
			}
		}

		/* 3. Calculate the possible enrollments */
		int toEnroll;
		toEnroll = class_.getCapacity() - class_.getEnrolled() - nRecWrites;

		/* 4. If we don't exceed capacity, add */
		if (toEnroll >= 0) {
			for (int i = 0; i < nRecWrites; i++) {
				String studentId = request.getWrites(i).getWrite().split(":")[0];
				String studentName = request.getWrites(i).getWrite().split(":")[1];
				try {
					class_.addStudentEnrolled(studentId, studentName);
				}
				catch (DuplicateEnrollmentException e) {
					cancelled.add(request.getWrites(i).getWrite());
				}
				catch (ClassFullException | ClosedForEnrollmentsException e) {
					debug("This shouldn't be happening.");
				}
			}
			if (toClose && !iClosed) {
				class_.setOpen(false);
			}
			return cancelled;
		}

		/* 5. Add until full */
		cancelled = addNEnrollments(toAdd, toAdd.size());

		return cancelled;
	}

	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

	/**
	 * Adds the possible enrollments, until the class is full.
	 * @param toAdd contains all the enrollments to be made
	 * @param nToAdd contains how many enrollments we want to
	 * make
	 * @return the enrollments which were cancelled
	 */
	private List<String> addNEnrollments(List<String> toAdd, int nToAdd) {
		List<String> cancelled = new ArrayList<>();
		for (int i = 0; i < nToAdd; i++) {
			String enrollment = toAdd.get(i);
			String studentId = enrollment.split(":")[0];
			String studentName = enrollment.split(":")[1];
			try {
				class_.addStudentEnrolled(studentId, studentName);
			}
			catch (DuplicateEnrollmentException e) {
				// only add to cancelled if it wasn't my write
				if (!isMyWrite(enrollment)) {
					cancelled.add(enrollment);
				}
			}
			catch (ClassFullException e) {
				cancelled.add(enrollment);
			}
			catch (ClosedForEnrollmentsException e) {
				debug("This shouldn't be happening.");
			}
		}
		return cancelled;
	}

	/**
	 * Determines if a certain enrollment was made by me.
	 * @param enrollment the enrollment in question
	 * @return true if I did it, false otherwise
	 */
	private boolean isMyWrite(String enrollment) {
		Collection<String> writes = classServerFrontend.get_writes().values();
		for (String write : writes) {
			if (write.equals(enrollment)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Calculates the first clock value for the writes sent by
	 * the other server
	 * @param request the propagateStateRequest I received
	 * @return the minimum time with a write
	 */
	private int getMinClock(PropagateStateRequest request) {
		int nRecWrites = request.getWritesCount();
		int recClockMin = -1;
		for (int i = 0; i < nRecWrites; i++) {
			int clock = request.getWrites(i).getClock();
			if (clock < recClockMin || recClockMin < 0) {
				recClockMin = clock;
			}
		}
		return recClockMin;
	}

	/**
	 * Auxiliary function to determine the operation performed
	 * on the other server at its own given time.
	 * @param request the propagateStateRequest I received
	 * @param time the clock's value the writing corresponds to
	 * @return the writing performed
	 */
	private String getWriteGivenTime(PropagateStateRequest request, int time) {
		String write = "";
		int nRecWrites = request.getWritesCount();
		for (int i = 0; i < nRecWrites; i++) {
			if (request.getWrites(i).getClock() == time) {
				write = request.getWrites(i).getWrite();
				break;
			}
		}
		return write;
	}

	/**
	 * Ensures the request containing a class state is valid.
	 * This guaranties we won't have odd class states.
	 * @param request the message containing a class state
	 */
	public void validateClassState(PropagateStateRequest request){
		_validate.validate_capacity(request.getClassState().getCapacity());

		request.getClassState().getDiscardedList()
				.forEach(student -> _validate.validate_enrollment(student.getStudentId(), student.getStudentName()));

		request.getClassState().getEnrolledList()
				.forEach(student -> _validate.validate_enrollment(student.getStudentId(), student.getStudentName()));
	}

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
}



/*

	def READY(vector):
		if foreach px not in failed, my_vector[x] != null:
			return TRUE;
		else:
			return FALSE;

	Initialization: failed = {}; my_vector[] = null;
	On fail (px): failed += {px};
	On IC.propose(value i): RB.send(pi, value i);
	On RB.deliver(pj, value j): my_vector[j] = value j;
	On READY(my_vector): Consensus.propose(my_vector);
	On Consensus.decide(vector): IC.decide(vector);


 */

/*

	Initialization: unordered = {}; ordered = {}; num_seq = 0;
	On AB.send(m): RB.send(m);
	On RB.deliver(m): unordered += {m};

	while TRUE do:
		wait for unordered \ ordered != {};
		num_seq += 1;
		Consensus[num_seq].propose(unordered \ ordered);
		wait for Consensus[num_seq].decide(next);
		ordered += {next}
		foreach m in next:
			AB.send(m)


 */

/*

	Initialization: pending = {}; executed = {}; proposals = {};
					num_seq = 0; decided[] = false;
	On request from client P: pending += {P};
	On "i am leader" and decided[num_seq] = true and pending \ executed != {}:
		next_request := chooseOne(pending \ executed);
		<next_state, answer> = execRequest(state, next_request);
		RB.send(<PROPOSAL, my_id, num_seq + 1, next_request, next_state, answer>)

	On RB.deliver(proposal = <PROPOSAL, my_id, num_seq + 1, next_request, next_state, answer>):
		proposals += {proposal}

	On exists(proposal = <PROPOSAL, id, ns, request, state, answer>) in proposals:
		if decided[num_seq] = true and ns = num_seq + 1 and id = leader then:
			Consensus[ns].propose(<PROPOSAL, id, ns, request, state, answer>);
			wait for Consensus[ns].decide(<PROPOSAL, id_out, ns_out, request_out, state_out, answer_out>);

			num_seq += 1;
			decided[num_seq] = true;
			state = state_out;
			executed += {request_out};
			send answer_out to client;


 */