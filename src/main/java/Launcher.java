import Util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Launcher {

    public static final String fileNameOfStoredPeers = "peers.txt";

    public static void main(String[] args) {
        int PORT = 18018;
        String IP_ADDRESS = "139.59.159.65";
        String name = "cooler name eye";
        String versionOfNode = "0.9";
        List<String> listOfDiscoveredPeers = Util.readPeersOfPersistentFile(fileNameOfStoredPeers);

        Logger log = Logger.getLogger("logger");

        ServerNode serverNode = new ServerNode(PORT, IP_ADDRESS, name, versionOfNode, listOfDiscoveredPeers, log);
        log.info("Launching serverSocket of name: " + name + " and of port: " + PORT);
        serverNode.launch();
    }
}
