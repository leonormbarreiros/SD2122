package pt.ulisboa.tecnico.classes.classserver;

import java.util.*;
import java.util.concurrent.*;

import io.grpc.Context;
import io.grpc.Status;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.classserver.exceptions.*;
import pt.ulisboa.tecnico.classes.Validate;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.*;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.exceptions.InvalidStudentException;
import static io.grpc.Status.INVALID_ARGUMENT;
import static pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode.*;
import static pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;

public class StudentServiceImpl extends StudentServiceGrpc.StudentServiceImplBase {
    private final ClassDomain _class = ClassDomain.getInstance();

    private boolean _debug; // debug flag

    private ClassServerFrontend classServerFrontend = ClassServerFrontend.getInstanceWithoutArgs();

    private final Validate _validate = new Validate();

    public StudentServiceImpl(boolean debug) { _debug = debug; }

    public boolean isDebug() { return _debug; }

    /**
     * Helper method to print debug messages.
     */
    private void debug(String debugMessage) {
        if (isDebug())
            System.err.println(debugMessage);
    }

    /* * * * * * * * * * * * * * * * * MESSAGES /EVENTS * * * * * * * * * * * * * * * * * * */

    /**
     * Implements the rpc listClass, from the service StudentService, defined in the Contract,
     * by sending the class's state.
     *
     * @param request the message received
     * @param responseObserver special interface for the server to call with its response
     */
    @Override
    public void listClass(ListClassRequest request, StreamObserver<ListClassResponse> responseObserver) {
        if (Context.current().isCancelled()) {
            responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Time's up!").asRuntimeException());
            return;
        }

        debug("Received a listClass request message.");

        // response contains a ResponseCode
        ListClassResponse.Builder builderResponse = ListClassResponse.newBuilder();

        try {
            // response contains a ClassState -> which is also a message
            ClassState.Builder classState = ClassState.newBuilder();

            synchronized (_class) {
                if (!_class.isActive()) {
                    throw new InactiveServerException();
                }

                builderResponse.setCode(OK);

                // classState contains capacity
                int capacity = _class.getCapacity();
                classState.setCapacity(capacity);

                // classState contains openEnrollments
                boolean openEnrollments = _class.isOpen();
                classState.setOpenEnrollments(openEnrollments);

                // classState contains enrolled students
                ConcurrentHashMap<String, String> enrolledStudents = _class.getStudentsEnrolled();

                enrolledStudents.forEach((studentId, studentName) -> classState.addEnrolled(Student.newBuilder().setStudentId(studentId).setStudentName(studentName).build()));

                // classState contains discarded students
                ConcurrentHashMap<String, String> discardedStudents = _class.getStudentsDiscarded();

                discardedStudents.forEach((studentId, studentName) -> classState.addDiscarded(Student.newBuilder().setStudentId(studentId).setStudentName(studentName).build()));
            }
            builderResponse.setClassState(classState.build());
        }
        catch (InactiveServerException e) {
            builderResponse.setCode(INACTIVE_SERVER);
            builderResponse.setClassState(ClassState.getDefaultInstance());
        }

        ListClassResponse response = builderResponse.build();

        debug("About to send a listClass response message with code: " + response.getCode());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Implements the rpc enroll, from the service StudentService, defined in the Contract,
     * by (1) enrolling the student passed in the message, if possible, and (2) sending a
     * code which indicates if the request executed successfully.
     *
     * @param request the message received
     * @param responseObserver special interface for the server to call with its response
     */
    @Override
    public void enroll(EnrollRequest request, StreamObserver<EnrollResponse> responseObserver) {
        if (Context.current().isCancelled()) {
            responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Time's up!").asRuntimeException());
            return;
        }

        try {
            debug("Received an enroll request message for student: "
                    + request.getStudent().getStudentId() + ", " + request.getStudent().getStudentName());

            String studentId = request.getStudent().getStudentId();
            String studentName = request.getStudent().getStudentName();

            _validate.validate_enrollment(studentId, studentName);

            EnrollResponse.Builder builderResponse = EnrollResponse.newBuilder();

            synchronized (_class) {
                if (!_class.isActive()) {
                    throw new InactiveServerException();
                }
                _class.addStudentEnrolled(studentId, studentName);
            }
            builderResponse.setCode(OK);

            // UPDATE LOGICAL CLOCK AND WRITES
            classServerFrontend.addWrite(classServerFrontend.getClock(), studentId + ":" + studentName);
            classServerFrontend.setClock(classServerFrontend.getClock() + 1);

            EnrollResponse response = builderResponse.build();

            debug("About to send an enroll response message with code: " + response.getCode());

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (InactiveServerException e) {
            EnrollResponse response = EnrollResponse.newBuilder().setCode(INACTIVE_SERVER).build();
            debug("About to send an enroll response message with code: " + response.getCode());

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (DuplicateEnrollmentException e) {
            EnrollResponse response = EnrollResponse.newBuilder().setCode(STUDENT_ALREADY_ENROLLED).build();
            debug("About to send an enroll response message with code: " + response.getCode());

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (ClassFullException e) {
            EnrollResponse response = EnrollResponse.newBuilder().setCode(FULL_CLASS).build();
            debug("About to send an enroll response message with code: " + response.getCode());

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (ClosedForEnrollmentsException e) {
            EnrollResponse response = EnrollResponse.newBuilder().setCode(ENROLLMENTS_ALREADY_CLOSED).build();
            debug("About to send an enroll response message with code: " + response.getCode());

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (InvalidStudentException e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

}
