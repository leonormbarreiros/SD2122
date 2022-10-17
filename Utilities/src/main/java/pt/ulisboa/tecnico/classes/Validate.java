package pt.ulisboa.tecnico.classes;

import pt.ulisboa.tecnico.classes.exceptions.InvalidCapacityException;
import pt.ulisboa.tecnico.classes.exceptions.InvalidStudentException;

public class Validate {
    public Validate() {}
    /**
     * Checks wether the arguments are valid studentId and studentName
     * @param id the studentId to verify
     * @param name the studentName to verify
     * @return true if both the name and the id are valid
     */
    public void validate_enrollment(String id, String name) throws InvalidStudentException {
        boolean valid = true;
        int lenId = id.length();
        if (lenId != 9) { valid = false; }
        String s = id.substring(0, 5);
        if (!s.equals("aluno")) { valid = false; }
        for (int i = 5; i < lenId; i++) {
            if (!Character.isDigit(id.charAt(i))) { valid = false; }
        }
        int lenName = name.length();
        if ((lenName < 3) || (lenName > 30)) { valid = false; }
        if (!valid) {
            throw new InvalidStudentException(id, name);
        }
    }

    /**
     * Checks wether the argument is a valid studentId
     * @param id the studentId to verify
     * @return true if the id is valid
     */
    public void validate_id(String id) throws InvalidStudentException {
        boolean valid = true;

        int lenId = id.length();
        if (lenId != 9) { valid = false; }
        String s = id.substring(0, 5);
        if (!s.equals("aluno")) { valid = false; }
        for (int i = 5; i < lenId; i++) {
            if (!Character.isDigit(id.charAt(i))) { valid = false; }
        }

        if (!valid) {
            throw new InvalidStudentException(id, " ");
        }
    }

    /**
     * Checks wether the argument is a valid studentName
     * @param name the studentName to verify
     * @return true if the name is valid
     */
    public void validate_name(String name) throws InvalidStudentException {
        boolean valid = true;

        int lenName = name.length();
        if ((lenName < 3) || (lenName > 30)) { valid = false; }

        if (!valid) {
            throw new InvalidStudentException(" ", name);
        }
    }

    /**
     * Checks if the argument  is a valid capacity.
     * @param capacity the capacity to be verified
     * @return true if it's a valid capacity value
     */
    public void validate_capacity(int capacity) throws InvalidCapacityException {
        if (capacity < 0) {
            throw new InvalidCapacityException(capacity);
        }
    }
}
