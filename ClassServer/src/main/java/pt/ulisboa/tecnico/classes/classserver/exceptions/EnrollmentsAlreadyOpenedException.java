package pt.ulisboa.tecnico.classes.classserver.exceptions;

public class EnrollmentsAlreadyOpenedException extends Exception{
    private boolean open;

    public EnrollmentsAlreadyOpenedException (boolean open) {
        this.open = open;
    }
    public boolean isOpen(){return open;}
}
