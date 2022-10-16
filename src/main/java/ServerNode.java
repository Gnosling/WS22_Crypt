import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ServerNode {

    private int PORT;
    private String IP_ADDRESS;
    private String name;
    private List<String> listOfDiscoveredPeers;
    private Logger log;

    private ExecutorService service;
    private ServerSocket serverSocket;
    private List<Socket> sockets = new ArrayList<>();

    public ServerNode(int PORT, String IP_ADDRESS, String name, List<String> listOfDiscoveredPeers, Logger log) {
        this.PORT = PORT;
        this.IP_ADDRESS = IP_ADDRESS;
        this.name = name;
        this.listOfDiscoveredPeers = listOfDiscoveredPeers;
        this.log = log;
    }

    public void launch() {
        log.info("launching ...");
        // TODO: sich das nochmals ansehen!
        service = Executors.newFixedThreadPool(50);

        try {
            serverSocket = new ServerSocket(PORT);
            // TODO: nicht hier hinzufügen!
            listOfDiscoveredPeers.add(InetAddress.getLocalHost().getHostAddress() + ":" + PORT);
            service.execute(new ServerListenerThread(this, serverSocket, service, sockets, log));
            log.info("launched");
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
                    log.info(" --- INFO --- "
                        + "\n Server-Address: " + InetAddress.getLocalHost().getHostAddress());

                } else if (("peers").equals(cmd)) {
                    StringBuilder peers = new StringBuilder();
                    for (String peer : listOfDiscoveredPeers) {
                        peers.append(peer + "\n");
                    }
                    log.info(" --- Peers --- \n" + peers);

                } else {
                    log.info("unknown command: " + cmd);
                }
            }
        } catch (IOException e) {
            // IOException from System.in is very very unlikely (or impossible)
            // and cannot be handled
        }
    }

    public void shutdown() {
        log.info("shutting down ...");

        if (serverSocket != null) {
            try {
                serverSocket.close();
                log.info("serversocket was closed");
            } catch (IOException e) {
                log.warning("Error while closing server socket: " + e.getMessage());
            }
        }

        for (Socket socket : sockets) {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.severe("Error while closing socket: " + e.getMessage());
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
        log.info("was shut down!");
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
