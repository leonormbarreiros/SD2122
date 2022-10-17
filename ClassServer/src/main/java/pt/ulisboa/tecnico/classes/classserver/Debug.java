package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer;

public class Debug {
    private static final String OPEN_REQUEST = "Received an openEnrollments request message.";
    private static final String OPEN_RESPONSE = "About to send an openEnrollments response message with code: ";
    private static final String CLOSE_REQUEST = "Received an closeEnrollments request message.";
    private static final String CLOSE_RESPONSE = "About to send an closeEnrollments response message with code: ";
    private static final String LIST_REQUEST = "Received a listClass request message.";
    private static final String LIST_RESPONSE = "About to send a listClass response message with code: ";

    public Debug() {

    }

    /**
     * Prints a message about program state.
     * Use it after receive a cancelEnrollment request.
     * @param request
     */
    public void debug_cancelEnrollmentRequest(ProfessorClassServer.CancelEnrollmentRequest request){
        System.err.println("Received a cancelEnrollment request message.");
    }

    /**
     * Prints a message about program state.
     * Use it before send a cancelEnrollment response.
     * @param response
     */
    public  void debug_cancelEnrollmentResponse(ProfessorClassServer.CancelEnrollmentResponse response){
        System.err.printf("About to send a cancelEnrollment response message with code: ");
        System.err.println(response.getCode());
    }

    /**
     * Outputs a small description of the openEnrollments request to be sent
     * @param request the before-mentioned request
     */
    public void debug_openEnrollmentsRequest(ProfessorClassServer.OpenEnrollmentsRequest request) {
        System.err.println(OPEN_REQUEST);
    }

    /**
     * Outputs a small description of the openEnrollments response received
     * @param response the before-mentioned response
     */
    public void debug_openEnrollmentsResponse(ProfessorClassServer.OpenEnrollmentsResponse response) {
        System.err.printf(OPEN_RESPONSE);
        System.err.println(response.getCode());
    }

    /**
     * Outputs a small description of the closeEnrollments request to be sent
     * @param request the before-mentioned request
     */
    public void debug_closeEnrollmentsRequest(ProfessorClassServer.CloseEnrollmentsRequest request) {
        System.err.println(CLOSE_REQUEST);
    }

    /**
     * Outputs a small description of the closeEnrollments response received
     * @param response the before-mentioned response
     */
    public void debug_closeEnrollmentsResponse(ProfessorClassServer.CloseEnrollmentsResponse response) {
        System.err.printf(CLOSE_RESPONSE);
        System.err.println(response.getCode());
    }

    /**
     * Outputs a small description of the listClass request to be sent
     * @param request the before-mentioned request
     */
    public void debug_listClassRequest(ProfessorClassServer.ListClassRequest request) {
        System.err.println(LIST_REQUEST);
    }

    /**
     * Outputs a small description of the listClass response received
     * @param response the before-mentioned response
     */
    public void debug_listClassResponse(ProfessorClassServer.ListClassResponse response) {
        System.err.printf(LIST_RESPONSE);
        System.err.println(response.getCode());
    }
}
