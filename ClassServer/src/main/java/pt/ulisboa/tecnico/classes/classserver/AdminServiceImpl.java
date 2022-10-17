package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.Context;
import io.grpc.Status;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;

import pt.ulisboa.tecnico.classes.classserver.exceptions.*;
import io.grpc.stub.StreamObserver;

import static pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode.*;


public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {
    private ClassDomain class_ = ClassDomain.getInstance();
    private static final boolean TIMER = true;
    private static final boolean COMMAND = false;

    private ClassServerFrontend classServerFrontend = ClassServerFrontend.getInstanceWithoutArgs();

    private boolean debug;

    private final Debug _debugger = new Debug();

    /**
     * Constructor
     * @param debug
     */
    public AdminServiceImpl(boolean debug){
        this.debug = debug;
    }

    /**
     * @return true if the debug mode is active, otherwise false
     */
    public boolean isDebug() { return debug; }

    /** Helper method to print debug messages. */
    private void debug(String debugMessage) {
        if (isDebug()) System.err.println(debugMessage);
    }

    /**
     * Process the activate request and send a response
     */
    @Override
    public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver){
        debug("Received a activate request message.");

        synchronized (class_) {
            class_.activate();
            synchronized (classServerFrontend){
                classServerFrontend.setActiveGossip(true);
            }
        }

        //build response
        ActivateResponse response = ActivateResponse.newBuilder().setCode(OK).build();

        debug("About to send a activate response message with code: " + response.getCode());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Process the deactivate request and send a response
     * @param request the request message received
     * @param responseObserver special interface for the server to call with its response
     */
    @Override
    public void deactivate(DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {
        debug("Received a deactivate request message.");

        synchronized (class_) {
            class_.deactivate();
            synchronized (classServerFrontend){
                classServerFrontend.setActiveGossip(false);
            }
        }

        //build response
        DeactivateResponse response = DeactivateResponse.newBuilder().setCode(OK).build();

        debug("About to send a deactivate response message with code: " + response.getCode());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Build ClassState
     * Note: this function is not syncronized, it should be syncronized when calling this function
     * @return ClassState
     */
    public ClassState.Builder buildClassState(){
        ClassState.Builder classStateBuilder = ClassState.newBuilder();
            // Build ClassState
            Student.Builder StudentBuilder = Student.newBuilder();
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
        return classStateBuilder;
    }

    /**
     * Process the dump request and send a response
     * @param request
     * @param responseObserver
     */
    @Override
    public void dump(DumpRequest request, StreamObserver<DumpResponse> responseObserver) {
        debug("Received a dump request message.");

        if (Context.current().isCancelled()) {
            responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Time's up!").asRuntimeException());
            return;
        }
        DumpResponse response;
        synchronized (class_){
            try{
                if(!class_.isActive()){
                    throw new InactiveServerException();
                }
                else{
                    // request has no parameters so no need to validate it!

                    ClassState.Builder classStateBuilder = buildClassState();

                    //build response
                    response = DumpResponse.newBuilder().setCode(OK).setClassState(classStateBuilder).build();
                }
            }
            catch (InactiveServerException e) {
                response = DumpResponse.newBuilder().setCode(INACTIVE_SERVER).build();
            }
        }

        debug("About to send a dump response message with code: " + response.getCode());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void activateGossip(ActivateGossipRequest request, StreamObserver<ActivateGossipResponse> responseObserver){
        debug("Received a activate_gossip request message.");

        if (Context.current().isCancelled()) {
            responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Time's up!").asRuntimeException());
            return;
        }
        ActivateGossipResponse response;
        synchronized (class_ ){
            try{
                if(!class_.isActive()){
                    throw new InactiveServerException();
                }
                else{
                    synchronized (classServerFrontend){
                        classServerFrontend.setActiveGossip(true);
                        response = ActivateGossipResponse.newBuilder().setCode(OK).build();
                    }
                }
            }
            catch (InactiveServerException e) {
                response = ActivateGossipResponse.newBuilder().setCode(INACTIVE_SERVER).build();
            }
        }

        debug("About to send a activate_gossip response message with code: " + response.getCode());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deactivateGossip(DeactivateGossipRequest request, StreamObserver<DeactivateGossipResponse> responseObserver){
        debug("Received a deactivate_gossip request message.");

        if (Context.current().isCancelled()) {
            responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Time's up!").asRuntimeException());
            return;
        }
        DeactivateGossipResponse response;
        synchronized (class_ ){
            try{
                if(!class_.isActive()){
                    throw new InactiveServerException();
                }
                else{
                    synchronized (classServerFrontend){
                        classServerFrontend.setActiveGossip(false);
                        response = DeactivateGossipResponse.newBuilder().setCode(OK).build();
                    }
                }
            }
            catch (InactiveServerException e) {
                response = DeactivateGossipResponse.newBuilder().setCode(INACTIVE_SERVER).build();
            }
        }

        debug("About to send a deactivate_gossip response message with code: " + response.getCode());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void gossip(GossipRequest request, StreamObserver<GossipResponse> responseObserver){
        debug("Received a gossip request message.");

        if (Context.current().isCancelled()) {
            responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Time's up!").asRuntimeException());
            return;
        }
        GossipResponse response;
        synchronized (class_ ){
            try{
                if(!class_.isActive()){
                    throw new InactiveServerException();
                }
                synchronized (classServerFrontend){
                    if (classServerFrontend.propagateState(COMMAND)) {
                        response = GossipResponse.newBuilder().setCode(OK).build();
                    }
                    else{
                        response = GossipResponse.newBuilder().setCode(WRITING_NOT_SUPPORTED).build();
                    }
                }
            }
            catch (InactiveServerException e) {
                response = GossipResponse.newBuilder().setCode(INACTIVE_SERVER).build();
            }
        }

        debug("About to send a gossip response message with code: " + response.getCode());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
