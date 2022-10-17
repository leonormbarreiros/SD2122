package pt.ulisboa.tecnico.classes.namingserver.exceptions;

public class UnknownServerException extends IllegalArgumentException {
    public UnknownServerException(String target) {
        super("Couldn't delete. " + target + " not found in ServerEntries!");
    }
}
