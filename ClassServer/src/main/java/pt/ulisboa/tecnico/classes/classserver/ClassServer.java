package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.util.*;
import java.io.IOException;


public class ClassServer {
    private static final String service = "Turmas";

    private static final boolean TIMER = true;
    private static final boolean COMMAND = false;

    /** Server host port. */
    private static int port;

    /** Server host */
    private static String host;

    /** Debugger mode*/
    private static boolean debug;

    private static String type;

    private static List<String> primaryServers = new ArrayList<>();

    private static List<String> secondaryServers = new ArrayList<>();

    public static void main(String[] args) throws IOException, InterruptedException {
        // check arguments
        if (args.length == 3 && (args[2].equals("P") || args[2].equals("S"))) {
            host = args[0];
            port = Integer.valueOf(args[1]);
            type = args[2];
            debug = false;
        }else if(args.length == 4 && args[3].equals("-debug") && (args[2].equals("P") || args[2].equals("S"))){
            host = args[0];
            port = Integer.valueOf(args[1]);
            type = args[2];
            debug = true;
        }else{
            System.err.printf("Usage: java %s <host> <port> <P|S> [-debug]", ClassServer.class.getName());
            return;
        }

        //Register the server
        NamingServerFrontend namingServerFrontend = new NamingServerFrontend(debug);
        namingServerFrontend.register(
                service, String.format("%s:%d", host, port), new ArrayList<String>(Arrays.asList(type)));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            namingServerFrontend.delete(service, host + ":" + Integer.toString(port));
        }));

        //lookup for other servers
        ClassServerFrontend frontend = null;
        switch (type){
            case "P":
                while(secondaryServers.size() <= 0){
                    secondaryServers = namingServerFrontend.lookup("Turmas", new ArrayList<>(List.of("S")));
                }

                frontend = ClassServerFrontend.getInstance(secondaryServers.get(0), debug);
                break;
            case "S":
                while(primaryServers.size() <= 0){
                    primaryServers = namingServerFrontend.lookup("Turmas", new ArrayList<>(List.of("P")));
                }

                frontend = ClassServerFrontend.getInstance(primaryServers.get(0), debug);
                break;
        }

        final BindableService adminService = new AdminServiceImpl(debug);
        final BindableService professorService = new ProfessorServiceImpl(debug, type);
        final BindableService studentService = new StudentServiceImpl(debug);

        final BindableService classServerService = new ClassServerServiceImpl(debug, type);

        // Create a new server to listen on port.
        Server server = ServerBuilder.forPort(port)
                .addService(adminService)
                .addService(professorService)
                .addService(studentService)
                .addService(classServerService)
                .build();

        // Start the server.
        server.start();

        //propagate state if primary server
        if(type.equals("P") || type.equals("S")){
            int MINUTES = 1; // The delay in minutes
            Timer timer = new Timer();
            ClassServerFrontend finalFrontend = frontend;
            System.err.println("PROP");
            timer.schedule(new TimerTask() {
                @Override
                public void run() { // Function runs every MINUTES minutes.
                    finalFrontend.propagateState(TIMER);
                }
            }, 0, 1000 * 60 * MINUTES);
            // 1000 milliseconds in a second * 60 per minute * the MINUTES variable.
        }

        // Do not exit the main thread. Wait until server is terminated.
        server.awaitTermination();
        namingServerFrontend.delete(service, Integer.toString(port));
    }

}
