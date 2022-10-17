package pt.ulisboa.tecnico.classes.namingserver.exceptions;

public class DuplicateServerException extends IllegalArgumentException{
	private final String name;
	private final String target;

	public DuplicateServerException(String name, String target) {
		super("Server with service \"" + name + "\" and target \"" + target + "\" already exist.");
		this.name = name;
		this.target = target;
	}

	public String getName() { return name; }

	public String getTarget() {
		return target;
	}
}
