package pt.ulisboa.tecnico.classes.namingserver.exceptions;

public class ServiceNotFoundException extends IllegalArgumentException {
    private final String name;

    public ServiceNotFoundException(String name) {
        super("Service with name " + name + " doesn't exist.");
        this.name = name;
    }

    public String getName() { return name; }
}
