import Entities.Object;
import Util.Util;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.swing.text.html.parser.Entity;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class Launcher {

    public static final String fileNameOfStoredPeers = "peers.txt";
    public static final String fileNameOfStoredObjects = "objects.txt";

    public static void main(String[] args) {
        int PORT = 18018;
        String IP_ADDRESS = "139.59.159.65";
        String name = "not node, hot implode";
        String versionOfNode = "1.5";
        List<String> listOfDiscoveredPeers = Util.readPeersOfPersistentFile(fileNameOfStoredPeers);
        HashMap<String,Object> listOfObjects = Util.readObjectsOfPersistentFile(fileNameOfStoredObjects);

        Security.addProvider(new BouncyCastleProvider());

        Logger log = Logger.getLogger("logger");

        if (listOfDiscoveredPeers == null) {
            log.severe("ERROR - peers could not be read from file!");
            System.exit(-1);
        }

        ServerNode serverNode = new ServerNode(PORT, IP_ADDRESS, name, versionOfNode, listOfDiscoveredPeers, listOfObjects, log);
        log.info("Launching serverSocket of name: " + name + " and of port: " + PORT);
        serverNode.launch();
    }
}
