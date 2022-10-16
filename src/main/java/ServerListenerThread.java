import Util.Util;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import messages.HelloMessage;
import messages.PeersMessage;

import javax.xml.stream.FactoryConfigurationError;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

public class ServerListenerThread extends Thread {

    private ServerNode serverNode;
    private ServerSocket serverSocket;
    private String name;
    private ExecutorService service;
    private List<Socket> sockets;

    private boolean wasGreeted = false;

    public ServerListenerThread(ServerNode serverNode, ServerSocket serverSocket, ExecutorService service, List<Socket> sockets) {
        this.serverNode = serverNode;
        this.serverSocket = serverSocket;
        this.service = service;
        this.sockets = sockets;
    }

    public void run() {
        Socket socket = null;
        boolean connectionOK = false;

        try {
            // wait for Client to connect
            socket = serverSocket.accept(); // throws only IOExc?
            sockets.add(socket);
            service.execute(new ServerListenerThread(serverNode, serverSocket, service, sockets));
            socket.setSoTimeout(1000*20);
            System.out.println("Connected to new client: " + socket.getInetAddress());

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

                System.out.println("[received]: " + request);
                boolean isJson = Util.isJson(request);

                if (!isJson || request == null || request.trim().equals("")) {
                    break;
                }

                // retrieve json
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(request);
                JsonNode typeNode = jsonNode.findValue("type");

                if (typeNode == null) {
                    break;
                }

                // retrieve type of the json-msg
                String type = typeNode.textValue();
                boolean badRequest = false;

                switch (type) {

                    case "hello":
                        // { "type" : "hello", "version" : "0.8.0", "agent" : "Kerma−Core Client 0.8" }
                        System.out.println("[Case: HELLO]");
                        HelloMessage receivedHello = objectMapper.readValue(request, HelloMessage.class);
                        if (!receivedHello.verifyHelloMessage()) {
                            badRequest = true;
                            break;
                        }
                        wasGreeted = true;
                        HelloMessage responseHello = new HelloMessage("hello", "0.8.0", "Kerma−Core Client 0.8");
                        response = objectMapper.writeValueAsString(responseHello);
                        break;

                    case "getpeers":
                        // { "type" : "getpeers" }
                        System.out.println("[Case: GETPEERS]");
                        if (!wasGreeted) {
                            System.out.println("was not greeted first!");
                            badRequest = true;
                            break;
                        }
                        // There is no need to create a json-object for the request --> DOCH!! TODO
                        
                        // peers are list stored in servernode
                        PeersMessage responsePeers = new PeersMessage("peers", serverNode.getListOfDiscoveredPeers());
                        response = objectMapper.writeValueAsString(responsePeers);
                        break;


                    default:
                        badRequest = true;
                        break;

                }

                if (badRequest) {
                    System.out.println("BAD REQUEST!");
                    break;
                }


                writer.println(response);
                writer.flush();
                System.out.println("[responded]: " + response);
            }

            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                    System.out.println("socket was closed");
                } catch (IOException e) {
                    // Ignored because we cannot handle it
                }
            }

        } catch (SocketException e) {
            // when the socket is closed, the I/O methods of the Socket will throw a SocketException
            // almost all SocketException cases indicate that the socket was closed
            System.err.println("socket was closed");

        } catch (SocketTimeoutException e) {
            // when the socket is closed, the I/O methods of the Socket will throw a SocketException
            // almost all SocketException cases indicate that the socket was closed
            System.err.println("socket did not respond in time");

        } catch (IOException e) {
            System.err.println("JSON Exception!");
            throw new UncheckedIOException(e);

        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                    System.out.println("socket was closed");
                } catch (IOException e) {
                    // Ignored because we cannot handle it
                }
            }
        }
    }
}
