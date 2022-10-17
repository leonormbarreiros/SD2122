package pt.ulisboa.tecnico.classes.classserver.exceptions;

public class DuplicateCancellationException extends Exception {
    private String student;

    public DuplicateCancellationException (String student) {
        this.student = student;
    }
    public String getStudent() {
        return this.student;
    }

}
