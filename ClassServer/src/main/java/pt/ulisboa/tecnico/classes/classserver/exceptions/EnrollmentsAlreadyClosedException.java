package pt.ulisboa.tecnico.classes.classserver.exceptions;

public class EnrollmentsAlreadyClosedException extends Exception {
    private boolean open;

    public EnrollmentsAlreadyClosedException (boolean open) {
        this.open = open;
    }
    public boolean isOpen(){return open;}
}
