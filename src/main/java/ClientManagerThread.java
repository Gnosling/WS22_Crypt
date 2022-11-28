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
        List<String> graders = new ArrayList<>();
        List<String> nonGraders = new ArrayList<>();

        for (String peer : peers) {
            if (peer.startsWith("128.130.122.101")) {
                graders.add(peer);
            } else {
                nonGraders.add(peer);
            }
        }

        // prioritize graders
        for (String grader : graders) {

            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            try {
                // start new ClientThread
                String[] parts = grader.split(":");
                if (parts.length < 2) {
                    log.warning(clientManagerLogMsg + "grader could not be parsed: " + grader);
                    continue;
                }
                int port = Integer.valueOf(parts[parts.length - 1]);
                String host = grader.substring(0, grader.lastIndexOf(":"));

                log.info(clientManagerLogMsg + "starting connection to grader: " + grader);
                service.execute(new ClientThread(host, port, serverNode, sockets, commands, log));


            } catch (Exception e) {
                log.warning(clientManagerLogMsg + "was terminated due to exception: " + e);
            }
        }

        int counter = 0;
        for (String nonGrader : nonGraders) {

            if (Thread.currentThread().isInterrupted() || counter > 1500) {
                break;
            }

            try {
                // start new ClientThread
                String[] parts = nonGrader.split(":");
                if (parts.length < 2) {
                    log.warning(clientManagerLogMsg + "nonGrader could not be parsed: " + nonGrader);
                    continue;
                }
                int port = Integer.valueOf(parts[parts.length - 1]);
                String host = nonGrader.substring(0, nonGrader.lastIndexOf(":"));

                log.info(clientManagerLogMsg + "starting connection to nonGrader: " + nonGrader);
                service.execute(new ClientThread(host, port, serverNode, sockets, commands, log));


            } catch (Exception e) {
                log.warning(clientManagerLogMsg + "was terminated due to exception: " + e);
            }
            counter++;
        }
    }
}
