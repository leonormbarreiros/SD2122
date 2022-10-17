package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.Validate;
import pt.ulisboa.tecnico.classes.classserver.exceptions.*;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.*;

import static io.grpc.Status.INVALID_ARGUMENT;
import static pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode.*;

import pt.ulisboa.tecnico.classes.exceptions.InvalidCapacityException;
import pt.ulisboa.tecnico.classes.exceptions.InvalidStudentException;

import static pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode.OK;
import static pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;


public class ProfessorServiceImpl extends ProfessorServiceGrpc.ProfessorServiceImplBase {
    private ClassDomain class_ = ClassDomain.getInstance();
    private boolean debug;
    private String type;

    private final Debug _debugger = new Debug();
    private final Validate _validate = new Validate();

    private ClassServerFrontend classServerFrontend = ClassServerFrontend.getInstanceWithoutArgs();

    /**
     * Constructor
     *
     * @param debug, debug option
     */
    public ProfessorServiceImpl(boolean debug, String type){
        this.debug = debug;
        this.type = type;
    }

    /**
     * Method to determine if the debug option is activate
     *
     * @return the value of the debug variable
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Method to determine the type of server
     *
     * @return the type of the server
     */
    public String getType() {
        return type;
    }

    /**
     * Process the openEnrollments request and send a response
     *
     * @param request
     * @param responseObserver
     */
    @Override
    public void openEnrollments(OpenEnrollmentsRequest request, StreamObserver<OpenEnrollmentsResponse> responseObserver){
        if(isDebug()){
            _debugger.debug_openEnrollmentsRequest(request);
        }

        try{
            synchronized (class_){
                //check is the server is active
                if(!class_.isActive()){
                    throw new InactiveServerException();
                }

                if(getType().equals("S")){
                    throw new WritingNotSupportedException();
                }

                _validate.validate_capacity(request.getCapacity());

                class_.openEnrollments(request.getCapacity());
            }

            classServerFrontend.addWrite(classServerFrontend.getClock(), "open" + ":" + String.valueOf(request.getCapacity()));
            classServerFrontend.setClock(classServerFrontend.getClock() + 1);

            OpenEnrollmentsResponse response = OpenEnrollmentsResponse.newBuilder().setCode(OK).build();

            if(isDebug()){
                _debugger.debug_openEnrollmentsResponse(response);
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch(InactiveServerException ex){
            OpenEnrollmentsResponse response = OpenEnrollmentsResponse.newBuilder().setCode(INACTIVE_SERVER).build();

            if(isDebug()){
                _debugger.debug_openEnrollmentsResponse(response);
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch(WritingNotSupportedException ex){
            OpenEnrollmentsResponse response = OpenEnrollmentsResponse.newBuilder().setCode(WRITING_NOT_SUPPORTED).build();

            if(isDebug()){
                _debugger.debug_openEnrollmentsResponse(response);
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch(EnrollmentsAlreadyOpenedException ex){
            OpenEnrollmentsResponse response = OpenEnrollmentsResponse.newBuilder().setCode(ENROLLMENTS_ALREADY_OPENED).build();

            if(isDebug()){
                _debugger.debug_openEnrollmentsResponse(response);
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch(FullClassException ex){
            OpenEnrollmentsResponse response = OpenEnrollmentsResponse.newBuilder().setCode(FULL_CLASS).build();

            if(isDebug()){
                _debugger.debug_openEnrollmentsResponse(response);
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (InvalidCapacityException e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }

    }

    /**
     * Process the closeEnrollments request and send a response
     *
     * @param request
     * @param responseObserver
     */
    @Override
    public void closeEnrollments(CloseEnrollmentsRequest request, StreamObserver<CloseEnrollmentsResponse> responseObserver){
        if(isDebug()){
            _debugger.debug_closeEnrollmentsRequest(request);
        }

        try{
            // request has no parameters so no need to validate it!

            synchronized (class_){
                //check is the server is active
                if(!class_.isActive()){
                    throw new InactiveServerException();
                }

                if(getType().equals("S")){
                    throw new WritingNotSupportedException();
                }

                class_.closeEnrollments();
            }

            classServerFrontend.addWrite(classServerFrontend.getClock(), "close" + ":" + "0");
            classServerFrontend.setClock(classServerFrontend.getClock() + 1);

            CloseEnrollmentsResponse response = CloseEnrollmentsResponse.newBuilder().setCode(OK).build();

            if(isDebug()){
                _debugger.debug_closeEnrollmentsResponse(response);
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch(InactiveServerException ex){
            CloseEnrollmentsResponse response = CloseEnrollmentsResponse.newBuilder().setCode(INACTIVE_SERVER).build();

            if(isDebug()){
                _debugger.debug_closeEnrollmentsResponse(response);
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch(WritingNotSupportedException ex){
            CloseEnrollmentsResponse response = CloseEnrollmentsResponse.newBuilder().setCode(WRITING_NOT_SUPPORTED).build();

            if(isDebug()){
                _debugger.debug_closeEnrollmentsResponse(response);
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch(EnrollmentsAlreadyClosedException ex){
            CloseEnrollmentsResponse response = CloseEnrollmentsResponse.newBuilder().setCode(ENROLLMENTS_ALREADY_CLOSED).build();

            if(isDebug()){
                _debugger.debug_closeEnrollmentsResponse(response);
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    /**
     * Process the listClass request and send a response
     *
     * @param request
     * @param responseObserver
     */
    @Override
    public void listClass(ListClassRequest request, StreamObserver<ListClassResponse> responseObserver){
        if(isDebug()){
            _debugger.debug_listClassRequest(request);
        }

        try{
            // request has no parameters so no need to validate it!

            ClassesDefinitions.ClassState.Builder classStateBuilder = ClassesDefinitions.ClassState.newBuilder();
            synchronized (class_) {
                //check is the server is active
                if(!class_.isActive()){
                    throw new InactiveServerException();
                }

                // Build ClassState
                ClassesDefinitions.Student.Builder StudentBuilder = ClassesDefinitions.Student.newBuilder();
                classStateBuilder.setCapacity(class_.getCapacity());
                classStateBuilder.setOpenEnrollments(class_.isOpen());
                class_.getStudentsDiscarded().forEach(
                        (id, name) ->
                                classStateBuilder.addDiscarded(
                                        StudentBuilder.setStudentId(id).setStudentName(name).build()));
                class_.getStudentsEnrolled().forEach(
                        (id, name) ->
                                classStateBuilder.addEnrolled(
                                        StudentBuilder.setStudentId(id).setStudentName(name).build()));
                classStateBuilder.build();
            }

            ListClassResponse response = ListClassResponse.newBuilder().setCode(OK).setClassState(classStateBuilder).build();

            if(isDebug()){
                _debugger.debug_listClassResponse(response);
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch(InactiveServerException e){
            ListClassResponse response = ListClassResponse.newBuilder().setCode(INACTIVE_SERVER).build();
            if(isDebug()){
                _debugger.debug_listClassResponse(response);
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }


    }

    /**
     * Process the cancelEnrollment request and send a response
     * @param request
     * @param responseObserver
     */
    @Override
    public void cancelEnrollment(CancelEnrollmentRequest request, StreamObserver<CancelEnrollmentResponse> responseObserver){
        if(isDebug()){
            _debugger.debug_cancelEnrollmentRequest(request);
        }

        try{
            if (Context.current().isCancelled()) {
                responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Time's up!").asRuntimeException());
                return;
            }

            //check is the server is active
            synchronized (class_){
                if(!class_.isActive()){
                    throw new InactiveServerException();
                }

                if(getType().equals("S")){
                    throw new WritingNotSupportedException();
                }

                _validate.validate_id(request.getStudentId());

                class_.cancelEnrollment(request.getStudentId());
            }

            CancelEnrollmentResponse response = CancelEnrollmentResponse.newBuilder().setCode(OK).build();
            if(isDebug()){
                _debugger.debug_cancelEnrollmentResponse(response);
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch(InactiveServerException e){
            CancelEnrollmentResponse response = CancelEnrollmentResponse.newBuilder().setCode(INACTIVE_SERVER).build();
            if(isDebug()){
                _debugger.debug_cancelEnrollmentResponse(response);
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch(WritingNotSupportedException ex){
            CancelEnrollmentResponse response = CancelEnrollmentResponse.newBuilder().setCode(WRITING_NOT_SUPPORTED).build();

            if(isDebug()){
                _debugger.debug_cancelEnrollmentResponse(response);
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch(UnknownEnrollmentException e){
            CancelEnrollmentResponse response = CancelEnrollmentResponse.newBuilder().setCode(ResponseCode.NON_EXISTING_STUDENT ).build();
            if(isDebug()){
                _debugger.debug_cancelEnrollmentResponse(response);
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (InvalidStudentException e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
