package pt.ulisboa.tecnico.classes.classserver.exceptions;

public class FullClassException extends Exception{
    private int capacity;

    public FullClassException (int capacity) {
        this.capacity = capacity;
    }
    public int getCapacity(){return capacity;}
}
