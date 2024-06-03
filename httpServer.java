import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class httpServer {
    private static final int NTHREADS = 100; // maximum threads for this programprivate static final ExecutorService

    // Open new server socket on port 8080
    public static void main(String args[]) throws IOException, Exception {
        ServerSocket serverSocket = new ServerSocket(8080);
        ExecutorService exec = Executors.newFixedThreadPool(NTHREADS);
        while (true) {
            Socket connection = serverSocket.accept();
            ServerListenerThread threadRunner = new ServerListenerThread(connection);
            exec.execute(threadRunner);
        }
    }
}
