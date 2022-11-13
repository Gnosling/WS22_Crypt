import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class ClientManagerThread extends Thread {

    private ServerNode serverNode;
    private ExecutorService service;
    private List<Socket> sockets;
    private String mode;
    private HashMap<String, List<String>> commands;
    private Logger log;

    private String clientManagerLogMsg;

    public ClientManagerThread(ServerNode serverNode, ExecutorService service, List<Socket> sockets,
                               String mode, HashMap<String, List<String>> commands, Logger log) {
        this.serverNode = serverNode;
        this.service = service;
        this.sockets = sockets;
        this.mode = mode;
        this.commands = commands;
        this.log = log;
        this.clientManagerLogMsg = "[client-manager] ";
    }

    public void run() {
        log.info(clientManagerLogMsg + "launched with mode:" + mode + "; cmds: " + commands);

        // it now tries to connect to all peers (ie. broadcast)
        List<String> peers = new ArrayList<>(serverNode.getListOfDiscoveredPeers());
        // Do it in two steps
        for (String peer : peers) {

            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            try {
                // start new ClientThread
                String[] parts = peer.split(":");
                if (parts.length < 2) {
                    log.warning(clientManagerLogMsg + "peer could not be parsed: " + peer);
                    continue;
                }
                int port = Integer.valueOf(parts[parts.length - 1]);
                String host = peer.substring(0, peer.lastIndexOf(":"));

                log.info(clientManagerLogMsg + "starting connection to peer: " + peer);
                service.execute(new ClientThread(host, port, serverNode, sockets, commands, log));


            } catch (Exception e) {
                log.warning(clientManagerLogMsg + "was terminated due to exception: " + e);
            }
        }
    }
}
