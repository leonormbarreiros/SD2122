package pt.ulisboa.tecnico.classes.classserver;

import java.util.Map;
import java.util.concurrent.*;
import pt.ulisboa.tecnico.classes.classserver.exceptions.*;

public class ClassDomain {
    // Static variable reference of instance of type Singleton
    // Singleton
    private static ClassDomain instance = null;

    private int capacity;

    private int enrolled = 0;

    private boolean open;

    private boolean active = true;

    private ConcurrentHashMap<String, String> studentsEnrolled = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, String> studentsDiscarded = new ConcurrentHashMap<>();

    /**
     * Constructor (Singleton)
     * Creating private constructor restricted to this class itself
     */
    private ClassDomain() {}

    /**
     * Static method to create instance of ClassDomain class
     * @return instance of class itself
     */
    public static ClassDomain getInstance(){
        if (instance == null)
            instance = new ClassDomain();

        return instance;
    }

    /**
     * @return number of enrolled student
     */
    public int getEnrolled() {
        return enrolled;
    }

    /**
     * Sets the number of enrolled student
     * @param enrolled number of enrolled student
     */
    public void setEnrolled(int enrolled) {
        this.enrolled = enrolled;
    }

    /**
     * @return capacity of the class
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Sets the capacity of the class
     * @param capacity
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * @return true if the enrollment is open, otherwise false
     */
    public boolean isOpen() {
            return open;
    }

    /**
     * Changes the enrollment between open (true) and close (false)
     * @param open
     */
    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @return collection of Enrolled Students
     */
    public ConcurrentHashMap<String, String> getStudentsEnrolled() {
        return studentsEnrolled;
    }

    public void setStudentsEnrolled(ConcurrentHashMap<String, String> studentsEnrolled) {
        this.studentsEnrolled = studentsEnrolled;
    }

    /**
     * @return collection of Discarded Students
     */
    public ConcurrentHashMap<String, String> getStudentsDiscarded() { return studentsDiscarded; }

    public void setStudentsDiscarded(ConcurrentHashMap<String, String> studentsDiscarded) {
        this.studentsDiscarded = studentsDiscarded;
    }

    public void addStudentEnrolled(String student_id, String student_name) throws DuplicateEnrollmentException, ClassFullException, ClosedForEnrollmentsException {
        // make sure enrollments are opened
        if (isOpen() == false) {
            throw new ClosedForEnrollmentsException();
        }

        // make sure student isn't already enrolled
        if (studentsEnrolled.containsKey(student_id)) {
            throw new DuplicateEnrollmentException(student_id);
        }

        // ensure student's enrollment doesn't exceed capacity
        if (getCapacity() == getEnrolled()) {
            throw new ClassFullException();
        }

        // if student's enrollment is discarded, uncanceled
        if (studentsDiscarded.containsKey(student_id)) {
            studentsDiscarded.remove(student_id);
        }
        setEnrolled(getEnrolled() + 1);
        studentsEnrolled.put(student_id, student_name);
    }

    /**
     * Removes a student from the enrolled student Set
     * @param student_id
     * @param student_name
     * @throws UnknownEnrollmentException when the student isn't enrolled
     */
    public void removeStudentEnrolled(String student_id, String student_name) throws UnknownEnrollmentException {
        // make sure student is currently enrolled
        if (!studentsEnrolled.containsKey(student_id)) {
            throw new UnknownEnrollmentException(student_id);
        }

        setEnrolled(getEnrolled() - 1);
        studentsEnrolled.remove(student_id, student_name);
    }

    /**
     * Adds a student from the discarded student Set,
     * also removes the student from the enrolled student Set
     * @param student_id
     * @param student_name
     * @throws UnknownEnrollmentException when the student isn't enrolled
     */
    public void addStudentDiscarded(String student_id, String student_name) throws UnknownEnrollmentException{
        removeStudentEnrolled(student_id, student_name);
        studentsDiscarded.put(student_id, student_name);
    }

    public void addNewDiscarded(String student_id, String student_name) throws DuplicateCancellationException {
        if (studentsDiscarded.containsKey(student_id)) {
            throw new DuplicateCancellationException(student_id);
        }
        studentsDiscarded.put(student_id, student_name);
    }

    /**
     * Removes a student from the discarded student Set
     * @param student_id
     * @param student_name
     * @throws UnknownCancellationException when the student isn't discarded
     */
    public void removeStudentDiscarded(String student_id, String student_name) throws UnknownCancellationException {
        // make sure student is currently Discarded
        if (!studentsDiscarded.containsKey(student_id)) {
            throw new UnknownCancellationException(student_id);
        }
        studentsDiscarded.remove(student_id);
    }

    public void openEnrollments(int capacity) throws EnrollmentsAlreadyOpenedException, FullClassException, InactiveServerException {
        if(!isActive()){
            throw new InactiveServerException();
        }
        else if(isOpen()){
            throw new EnrollmentsAlreadyOpenedException(isOpen());
        }
        else if(capacity<=studentsEnrolled.size()){
            throw new FullClassException(capacity);
        }
        setCapacity(capacity);
        setOpen(true);
    }

    public void closeEnrollments() throws EnrollmentsAlreadyClosedException, InactiveServerException {
        if(!isActive()){
            throw new InactiveServerException();
        }
        else if(!isOpen()){
            throw new EnrollmentsAlreadyClosedException(isOpen());
        }
        setOpen(false);
    }

    /**
     * Cancel a student's enrollments,
     * by adding the student in the discarded student Set
     * @param student_id
     * @throws UnknownEnrollmentException when the student isn't enrolled
     */
    public void cancelEnrollment(String student_id) throws UnknownEnrollmentException, InactiveServerException{
        if(!isActive()){
            throw new InactiveServerException();
        }

        addStudentDiscarded(student_id, studentsEnrolled.get(student_id));
    }

    public void activate(){
        setActive(true);
    }

    public void deactivate() {
        setActive(false);
    }

    @Override
    public String toString() {
        return "ClassDomain{" +
                "capacity=" + capacity +
                ", enrolled=" + enrolled +
                ", open=" + open +
                ", active=" + active +
                ", studentsEnrolled=" + studentsEnrolled +
                ", studentsDiscarded=" + studentsDiscarded +
                '}';
    }
}