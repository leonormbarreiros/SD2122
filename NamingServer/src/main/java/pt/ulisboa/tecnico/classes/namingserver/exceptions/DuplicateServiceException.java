package pt.ulisboa.tecnico.classes.namingserver.exceptions;

public class DuplicateServiceException extends IllegalArgumentException{

	private final String name;

	public DuplicateServiceException(String name) {
		super("Service with name " + name + " already exist.");
		this.name = name;
	}

	public String getName() { return name; }
}
