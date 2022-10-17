package pt.ulisboa.tecnico.classes.professor;

import pt.ulisboa.tecnico.classes.NamingServerFrontend;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Professor {

  private static NamingServerFrontend namingServerFrontend;

  private static final String EXIT_CMD = "exit";
  private static final String OPEN_CMD = "openEnrollments";
  private static final String CLOSE_CMD = "closeEnrollments";
  private static final String CANCEL_CMD = "cancelEnrollment";
  private static final String LIST_CMD = "list";

  /**
   * Main
   *
   * @param args, list of arguments
   */
  public static void main(String[] args) {
    boolean debug;
    ClassServerFrontend frontend;

    // check arguments
    if (args.length == 0) {
      debug = false;
    }
    else if (args.length == 1 && args[0].equals("-debug")) {
      debug = true;
    }
    else {
      System.out.printf("Usage: java %s [-debug]", Professor.class.getName());
      return;
    }

    namingServerFrontend = new NamingServerFrontend(debug);
    namingServerFrontend.createChannel("localhost", 5000);
    namingServerFrontend.createBlockingStub();
    List<String> primaryServers = namingServerFrontend.lookup("Turmas", new ArrayList<>(List.of("P")));
    List<String> secondaryServers = namingServerFrontend.lookup("Turmas", new ArrayList<>(List.of("S")));

    frontend = new ClassServerFrontend(debug, primaryServers, secondaryServers);

    Scanner scanner = new Scanner(System.in);
    while (true) {
      System.out.printf("> ");
      String line = scanner.nextLine();
      String[] lineSplit = line.split(" ", 0);

      // exit
      if (EXIT_CMD.equals(line)) {
        scanner.close();
        break;
      }
      else if (OPEN_CMD.equals(lineSplit[0])) {
        try{
          int number = Integer.parseInt(lineSplit[1]);
          System.out.println(frontend.openEnrollments(number));
        }
        catch (NumberFormatException ex){
          System.err.println("Enter a valid capacity! Try again.");
        }
        catch (ArrayIndexOutOfBoundsException ex){
          System.err.println("Missing Arguments! Try Again.");
        }
      }
      else if (CLOSE_CMD.equals(line)) {
        System.out.println(frontend.closeEnrollments());
      }
      else if (LIST_CMD.equals(line)) {
        frontend.listClass();
      }
      else if(CANCEL_CMD.equals(lineSplit[0])){
        try{
          System.out.println(frontend.cancelEnrollment(lineSplit[1]));
        }
        catch (ArrayIndexOutOfBoundsException ex){
          System.err.println("Missing Arguments! Try Again.");
        }
      }
    }

    namingServerFrontend.terminate();
    System.exit(1);

  }
}
