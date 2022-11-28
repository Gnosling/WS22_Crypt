import Entities.Object;
import Util.Util;
import Util.ContainerOfUTXO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static Util.Util.appendUTXOOnPersistentFileForHashOfBlock;

public class ServerNode {

    private int PORT;
    private String IP_ADDRESS;
    private String name;
    private String versionOfNode;
    private List<String> listOfDiscoveredPeers;
    private HashMap<String, Object> listOfObjects;
    private Logger log;

    private ExecutorService service;
    private ServerSocket serverSocket;
    private List<Socket> sockets = new ArrayList<>();
    private String serverAddress;

    public ServerNode(int PORT, String IP_ADDRESS, String name, String versionOfNode,
                      List<String> listOfDiscoveredPeers, HashMap<String, Object> listOfObjects, Logger log) {
        this.PORT = PORT;
        this.IP_ADDRESS = IP_ADDRESS;
        this.name = name;
        this.versionOfNode = versionOfNode;
        this.listOfDiscoveredPeers = listOfDiscoveredPeers;
        this.listOfObjects = listOfObjects;
        this.log = log;
    }

    public void launch() {
        log.info("launching ...");
        service = Executors.newFixedThreadPool(150);

        try {
            serverSocket = new ServerSocket(PORT);
            this.serverAddress = IP_ADDRESS + ":" + PORT;

            service.execute(new ServerListenerThread(this, serverSocket, service, sockets, log));
            HashMap<String, List<String>> cmds = new HashMap<>();
//            service.execute(new ClientManagerThread(this, service, sockets, "", cmds, log));
            log.info("launched");
        } catch (IOException e) {
            throw new UncheckedIOException("Error while server socket: ", e);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {
            // read commands from the console
            while (true) {
                String cmd = reader.readLine();

                if (cmd == null) {
                    continue;
                }

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

                } else if (("objects").equals(cmd)) {
                    StringBuilder objects = new StringBuilder();
                    for (Map.Entry<String, Object> entry : listOfObjects.entrySet()) {
                        objects.append(entry.getKey() + ":" + entry.getValue().toString() + "\n");
                    }
                    log.info(" --- Objects --- \n" + objects);

                } else if (cmd.startsWith("connect to ")) {
                    String[] parts = cmd.split(" ");
                    if (parts.length != 4) {
                        log.warning("bad command; expected 'connect to <host> <port>");
                        continue;
                    }
                    String host = parts[2];
                    String portString = parts[3];
                    int port = 0;
                    try {
                        port = Integer.valueOf(portString);
                    } catch (NumberFormatException numberFormatException) {
                        log.warning("wrong port; expected 'connect to <host> <port>");
                    }
                    HashMap<String, List<String>> commands = new HashMap<>();
                    service.execute(new ClientThread(host, port, this, sockets, commands, log));

//                } else if (cmd.equals("start clientmanager")) {
//                    // TODO: start new clientmanager?

                } else if (cmd.equals("reload persistent peers")) {
                    listOfDiscoveredPeers = Util.readPeersOfPersistentFile(Launcher.fileNameOfStoredPeers);
                    log.info("read persistent peers");

                } else {
                    log.warning("unknown command: " + cmd);
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

    public String getVersionOfNode() { return versionOfNode; }

    public List<String> getListOfDiscoveredPeers() {
        return listOfDiscoveredPeers;
    }

    public void setListOfDiscoveredPeers(List<String> listOfDiscoveredPeers) {
        this.listOfDiscoveredPeers = listOfDiscoveredPeers;
    }

    public HashMap<String, Object> getListOfObjects() {
        return listOfObjects;
    }

    public void setListOfObjects(HashMap<String, Object> listOfObjects) {
        this.listOfObjects = listOfObjects;
    }

    public synchronized String updateListOfDiscoveredPeers(List<String> receivedPeers) {

        String updatedInfo = "";
        List<String> knownPeers = Util.readPeersOfPersistentFile(Launcher.fileNameOfStoredPeers);
        if (knownPeers == null) {
            return null;
        }

        for (String peer : receivedPeers) {
            peer = peer.trim();
            if (peer.equals(serverAddress)) {
                continue;
            }
            if (!knownPeers.contains(peer)) {
                knownPeers.add(peer);
                updatedInfo += "new peer: " + peer + ", ";
            }
        }

        if (!updatedInfo.equals("")) {
            listOfDiscoveredPeers = knownPeers;
            Util.storePeersOnPersistentFile(knownPeers, Launcher.fileNameOfStoredPeers);
        }

        return updatedInfo;
    }

    public synchronized String appendToObjects(HashMap<String, Object> receivedObjects) {
        String updatedInfo = "";
        HashMap<String, Object> knownObjects = Util.readObjectsOfPersistentFile(Launcher.fileNameOfStoredObjects, Launcher.fileNameOfStoredUTXOs);
        HashMap<String, Object> newObjects = new HashMap<>();
        if (knownObjects == null) {
            return null;
        }

        for (Map.Entry<String, Object> object : receivedObjects.entrySet()) {
            if (!knownObjects.containsKey(object.getKey())) {
                newObjects.put(object.getKey(), object.getValue());
                listOfObjects.put(object.getKey(), object.getValue());
                updatedInfo += "new object: " + object.getKey() + ", ";
            }
        }

        if (!updatedInfo.equals("")) {
            Util.appendObjectsOnPersistentFile(newObjects, Launcher.fileNameOfStoredObjects);
        }

        return updatedInfo;
    }

    // assumes validation and everything was done beforehand
    public synchronized String appendToUTXOForNewHash(String hashOfBlock, HashMap<String, List<ContainerOfUTXO>> utxo) {

        String updatedInfo = "";
        if (appendUTXOOnPersistentFileForHashOfBlock(Launcher.fileNameOfStoredUTXOs, hashOfBlock, utxo)) {
            updatedInfo = "new UTXO for block " + hashOfBlock;
        }
        return updatedInfo;
    }

    public ExecutorService getService() {
        return service;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public List<Socket> getSockets() {
        return sockets;
    }

    public String getServerAddress() {
        return serverAddress;
    }
}
