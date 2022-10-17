package pt.ulisboa.tecnico.classes.classserver.exceptions;

public class DuplicateEnrollmentException extends Exception {
    private String student;

    public DuplicateEnrollmentException (String student) {
        this.student = student;
    }
    public String getStudent() {
        return this.student;
    }

}
