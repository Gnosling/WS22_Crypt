import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class ClientManagerThread extends Thread {

    private ServerNode serverNode;
    private ExecutorService service;
    private List<Socket> sockets;
    private Logger log;

    private String clientManagerLogMsg;

    public ClientManagerThread(ServerNode serverNode, ExecutorService service, List<Socket> sockets, Logger log) {
        this.serverNode = serverNode;
        this.service = service;
        this.sockets = sockets;
        this.log = log;
        this.clientManagerLogMsg = "[client-manager] ";
    }

    public void run() {
        log.info(clientManagerLogMsg + "launched");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                if (true) {
                    // start new ClientThread
                    String peer = serverNode.getListOfDiscoveredPeers().get(new Random().nextInt(serverNode.getListOfDiscoveredPeers().size()));
                    String[] parts = peer.split(":");
                    if (parts.length < 2) {
                        log.warning(clientManagerLogMsg + "peer could not be parsed: " + peer);
                        continue;
                    }
                    int port = Integer.valueOf(parts[parts.length-1]);
                    String host = peer.substring(0, peer.lastIndexOf(":"));
                    List<String> commands = new ArrayList<>();

                    log.info(clientManagerLogMsg + "starting connection to peer: " + peer);
                    service.execute(new ClientThread(host, port, serverNode, sockets, commands, log));
                }

                Thread.sleep(1000 * 60 * 60); // wait 60 min

            } catch (Exception e) {
                log.warning(clientManagerLogMsg + "was terminated due to exception: " + e);
            }
        }
    }

}
