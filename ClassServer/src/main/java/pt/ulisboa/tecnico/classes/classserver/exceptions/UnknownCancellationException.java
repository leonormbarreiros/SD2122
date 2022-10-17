package pt.ulisboa.tecnico.classes.classserver.exceptions;

public class UnknownCancellationException extends Exception {
    private String student;

    public UnknownCancellationException(String student) {
        this.student = student;
    }

    public String getStudent() {
        return this.student;
    }

}
