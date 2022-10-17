package pt.ulisboa.tecnico.classes.admin;

import pt.ulisboa.tecnico.classes.NamingServerFrontend;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Admin {

    private static final String EXIT_CMD = "exit";
    private static final String ACTIVATE_CMD = "activate";
    private static final String DEACTIVATE_CMD = "deactivate";
    private static final String ACTIVATE_GOSSIP_CMD = "activate_gossip";
    private static final String DEACTIVATE_GOSSIP_CMD = "deactivate_gossip";
    private static final String GOSSIP_CMD = "gossip";
    private static final String DUMP_CMD = "dump";

    public static void main(String[] args) {
        boolean debug;

        // check arguments
        if (args.length == 0) {
            debug = false;
        }else if(args.length == 1 && args[0].equals("-debug")){
            debug = true;
        }else{
            System.err.printf("Usage: java %s [-debug]", Admin.class.getName());
            return;
        }

        //lookup servers
        NamingServerFrontend namingServerFrontend = new NamingServerFrontend(debug);
        namingServerFrontend.createChannel("localhost", 5000);
        namingServerFrontend.createBlockingStub();
        List<String> primaryServers = namingServerFrontend.lookup("Turmas", new ArrayList<>(List.of("P")));
        List<String> secondaryServers = namingServerFrontend.lookup("Turmas", new ArrayList<>(List.of("S")));

        ClassServerFrontend frontend = new ClassServerFrontend(debug, primaryServers, secondaryServers);

        //read and execute commands
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.printf("> ");
            String line = scanner.nextLine();
            String[] lineSplit = line.split(" ", 0);

            // exit
            if (EXIT_CMD.equals(line)) {
                scanner.close();
                break;
            }
            else if (ACTIVATE_CMD.equals(lineSplit[0])) {
                if (lineSplit.length > 1) {
                    System.out.println(frontend.activate(lineSplit[1]));
                }else{
                    System.out.println(frontend.activate("P"));
                }
            }
            else if (DEACTIVATE_CMD.equals(lineSplit[0])) {
                if (lineSplit.length > 1) {
                    System.out.println(frontend.deactivate(lineSplit[1]));
                } else {
                    System.out.println(frontend.deactivate("P"));
                }
            }
            else if (ACTIVATE_GOSSIP_CMD.equals(lineSplit[0])) {
                if (lineSplit.length > 1) {
                    System.out.println(frontend.activate_gossip(lineSplit[1]));
                } else {
                    System.out.println(frontend.activate_gossip("P"));
                }
            }
            else if (DEACTIVATE_GOSSIP_CMD.equals(lineSplit[0])) {
                if (lineSplit.length > 1) {
                    System.out.println(frontend.deactivate_gossip(lineSplit[1]));
                } else {
                    System.out.println(frontend.deactivate_gossip("P"));
                }
            }
            else if (GOSSIP_CMD.equals(lineSplit[0])) {
                if (lineSplit.length > 1) {
                    System.out.println(frontend.gossip(lineSplit[1]));
                } else {
                    System.out.println(frontend.gossip("P"));
                }
            }
            else if (DUMP_CMD.equals(lineSplit[0])){
                if (lineSplit.length > 1) {
                    System.out.println(frontend.dump(lineSplit[1]));
                }else{
                    System.out.println(frontend.dump("P"));
                }
            }
        }
        namingServerFrontend.terminate();
        System.exit(1);

    }
}
