import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerNode {

    private int PORT;
    private String IP_ADDRESS;
    private String name;
    private List<String> listOfDiscoveredPeers;

    private ExecutorService service;
    private ServerSocket serverSocket;
    private List<Socket> sockets = new ArrayList<>();

    public ServerNode(int PORT, String IP_ADDRESS, String name, List<String> listOfDiscoveredPeers) {
        this.PORT = PORT;
        this.IP_ADDRESS = IP_ADDRESS;
        this.name = name;
        this.listOfDiscoveredPeers = listOfDiscoveredPeers;
    }

    public void launch() {
        System.out.println("launching ...");
        // TODO: sich das nochmals ansehen!
        service = Executors.newFixedThreadPool(50);

        try {
            serverSocket = new ServerSocket(PORT);
            listOfDiscoveredPeers.add(serverSocket.getInetAddress().getHostAddress() + ":" + PORT);
            service.execute(new ServerListenerThread(this, serverSocket, service, sockets));
            System.out.println("launched");
        } catch (IOException e) {
            throw new UncheckedIOException("Error while server socket: ", e);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {
            // read commands from the console
            while (true) {
                String cmd = reader.readLine();

                // close sockets and listening threads
                if (("shutdown").equals(cmd)) {
                    this.shutdown();
                    break;

                } else if (("info").equals(cmd)) {
                    System.out.println(" --- INFO --- " +
                            "\n" + serverSocket.getInetAddress());

                } else if (("peers").equals(cmd)) {
                    StringBuilder peers = new StringBuilder();
                    for (String peer : listOfDiscoveredPeers) {
                        peers.append(peer + "\n");
                    }
                    System.out.println(" --- Peers --- \n" + peers);

                } else {
                    System.out.println("unknown command: " + cmd);
                }
            }
        } catch (IOException e) {
            // IOException from System.in is very very unlikely (or impossible)
            // and cannot be handled
        }
    }

    public void shutdown() {
        System.out.println("shutting down ...");

        if (serverSocket != null) {
            try {
                serverSocket.close();
                System.out.println("serversocket was closed");
            } catch (IOException e) {
                System.err.println("Error while closing server socket: " + e.getMessage());
            }
        }

        for (Socket socket : sockets) {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error while closing socket: " + e.getMessage());
                }
            }
        }

        service.shutdown();
        try {
            if (!service.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
        }
        System.out.println("was shut down!");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getListOfDiscoveredPeers() {
        return listOfDiscoveredPeers;
    }

    public void setListOfDiscoveredPeers(List<String> listOfDiscoveredPeers) {
        this.listOfDiscoveredPeers = listOfDiscoveredPeers;
    }
}
