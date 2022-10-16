import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Launcher {

    public static void main(String[] args) {
        int PORT = 18018;
        String IP_ADDRESS = "";
        String name = "cooler name eye";
        List<String> listOfDiscoveredPeers = new ArrayList<>();
        listOfDiscoveredPeers.add("localhost:18018");

        Logger log = Logger.getLogger("logger");

        ServerNode serverNode = new ServerNode(PORT, IP_ADDRESS, name, listOfDiscoveredPeers, log);
        log.info("Launching serverSocket of name: " + name + " and of port: " + PORT);
        serverNode.launch();
    }
}
