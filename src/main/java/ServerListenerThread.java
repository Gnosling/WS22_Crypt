import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ServerListenerThread extends Thread {

    private ServerSocket serverSocket;
    private String name;
    private ExecutorService service;
    private List<Socket> sockets;

    public ServerListenerThread(ServerSocket serverSocket, ExecutorService service, List<Socket> sockets) {
        this.serverSocket = serverSocket;
        this.service = service;
        this.sockets = sockets;
    }

    public void run() {
        Socket socket = null;
        try {
            // wait for Client to connect
            socket = serverSocket.accept();
            sockets.add(socket);
            service.execute(new ServerListenerThread(serverSocket, service, sockets));

            // prepare the input reader for the socket
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // prepare the writer for responding to clients requests
            PrintWriter writer = new PrintWriter(socket.getOutputStream());

            String request = "";
            String response = "";

            // read client requests
            while (!Thread.currentThread().isInterrupted() && (request = reader.readLine()) != null) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                // TODO what about json?

                String[] parts = request.split("\\s"); //splits in spaces

                if (("hello").equals(parts[0])) {
                    response = "hello";
                }
                writer.println(response);
                writer.flush();
            }

        } catch (SocketException e) {
            // when the socket is closed, the I/O methods of the Socket will throw a SocketException
            // almost all SocketException cases indicate that the socket was closed
            System.err.println("socket was closed");

        } catch (IOException e) {
            throw new UncheckedIOException(e);

        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignored because we cannot handle it
                }
            }
        }
    }
}
