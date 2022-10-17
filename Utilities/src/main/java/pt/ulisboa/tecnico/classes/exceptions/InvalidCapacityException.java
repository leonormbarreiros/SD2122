package pt.ulisboa.tecnico.classes.exceptions;

public class InvalidCapacityException extends IllegalArgumentException {
    private final int capacity;

    public InvalidCapacityException(int capacity) {
        super("Wrong capacity value: " + capacity);
        this.capacity = capacity;
    }

    public int getCapacity() { return capacity; }
}
