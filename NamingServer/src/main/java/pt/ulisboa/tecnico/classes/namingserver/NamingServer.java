package pt.ulisboa.tecnico.classes.namingserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public class NamingServer {

  private static int port;
  private static String host;

  /** Debugger mode*/
  private static boolean debug;
  public static void main(String[] args) throws IOException, InterruptedException {
    // check arguments
    if (args.length == 2) {
      host = args[0];
      port = Integer.valueOf(args[1]);
      debug = false;
    }else if(args.length == 3 && args[2].equals("-debug")){
      host = args[0];
      port = Integer.valueOf(args[1]);
      debug = true;
    }else{
      System.out.printf("Usage: java %s <host> <port> [-debug]", NamingServer.class.getName());
      return;
    }
    final BindableService NamingServerService = new NamingServerServiceImpl(debug);

    // Create a new server to listen on port.
    Server namingServer = ServerBuilder.forPort(port).addService(NamingServerService).build();

    // Start the server.
    namingServer.start();

    // Do not exit the main thread. Wait until server is terminated.
    namingServer.awaitTermination();


  }
}
