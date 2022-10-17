package pt.ulisboa.tecnico.classes.exceptions;

public class InvalidStudentException extends IllegalArgumentException {
    private final String name;
    private final String id;

    public InvalidStudentException(String id, String name) {
        super("Either id (" + id + ") or name (" + name + ") are invalid.");
        this.name = name;
        this.id = id;
    }

    public String getName() { return name; }

    public String getId() { return id; }
}
