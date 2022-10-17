package pt.ulisboa.tecnico.classes.student;

import java.util.ArrayList;
import java.util.Scanner;

import pt.ulisboa.tecnico.classes.NamingServerFrontend;
import pt.ulisboa.tecnico.classes.Validate;
import pt.ulisboa.tecnico.classes.exceptions.InvalidStudentException;
import java.util.List;

public class Student {

  private static NamingServerFrontend namingServerFrontend;

  private final List<String> primaryServers = new ArrayList<>();
  private final List<String> secondaryServers = new ArrayList<>();

  private static boolean debug; // determins if debug option is activated
  private static String id; // student's id
  private static String name; // student's name
  private static final Validate validate = new Validate();

  public static boolean isDebug() {
    return debug;
  }

  public static void setDebug(boolean debug) {
    Student.debug = debug;
  }

  public static String getId() {
    return id;
  }

  public static void setId(String id) {
    Student.id = id;
  }

  public static String getName() {
    return name;
  }

  public static void setName(String name) {
    Student.name = name;
  }

  private static final String EXIT_CMD = "exit";
  private static final String LIST_CMD = "list";
  private static final String ENROLL_CMD = "enroll";

  /**
   * Evaluates wether the argument is the -debug flag and sets it accordingly.
   * 
   * @param arg the argument which may be the flag
   * @return true if it's the flag
   */
  public static boolean checkForDebug(String arg) {
    boolean contains = (arg.equals("-debug"));
    setDebug(contains);
    return isDebug();
  }

  public static void main(String[] args) {
    // check arguments
    if (args.length < 2) {
      System.out.println("Argument(s) missing!");
      System.out.printf("Usage: java %s alunoXXXX name [-debug]%n", Student.class.getName());
      return;
    }

    // parse arguments
    String id = args[0];
    String name = args[1];
    if (args.length > 2) {
      int max;
      if (checkForDebug(args[args.length - 1])) { max = args.length - 1; }
      else { max = args.length; }

      for (int i = 2; i < max; i++) {
        name = name + " " + args[i];
      }
    }

    try {
      validate.validate_enrollment(id, name);
    }
    catch (InvalidStudentException e) {
      System.err.println("Wrong format for argument(s)!");
      return;
    }

    setId(id);
    setName(name);

    namingServerFrontend = new NamingServerFrontend(debug);
    namingServerFrontend.createChannel("localhost", 5000);
    namingServerFrontend.createBlockingStub();
    List<String> primaryServers = namingServerFrontend.lookup("Turmas", new ArrayList<>(List.of("P")));
    List<String> secondaryServers = namingServerFrontend.lookup("Turmas", new ArrayList<>(List.of("S")));

    ClassServerFrontend classServerFrontend = new ClassServerFrontend(debug, primaryServers, secondaryServers);

    // read and execute commands
    Scanner scanner = new Scanner(System.in);
    while (true) {
      System.out.printf("> ");

      String line = scanner.nextLine();

      // exit
      if (EXIT_CMD.equals(line)) {
        scanner.close();
        break;
      }

      // list
      else if (LIST_CMD.equals(line)) {
        classServerFrontend.listClass();
      }

      // enroll
      else if (ENROLL_CMD.equals(line)) {
        classServerFrontend.enroll(getId(), getName());
      }
    }

    namingServerFrontend.terminate();
    System.exit(1);
  }
}
