package pt.ulisboa.tecnico.classes.classserver.exceptions;

public class UnknownEnrollmentException extends Exception {
    private String student;

    public UnknownEnrollmentException (String student) {
        this.student = student;
    }
    public String getStudent() {
        return this.student;
    }

}
