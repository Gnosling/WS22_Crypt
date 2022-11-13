import Entities.Object;
import Entities.Transaction;
import Util.Util;
import Util.TransactionSerializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import messages.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static Util.Util.*;
import static Util.Util.error;

public class ClientThread extends Thread {

    private String host;
    private int port;
    private ServerNode serverNode;
    private List<Socket> sockets;
    private HashMap<String, List<String>> commands;
    private Logger log;

    private Socket clientSocket;
    private String clientLogMsg;
    private boolean wasGreeted = false;


    public ClientThread(String host, int port, ServerNode serverNode, List<Socket> sockets, HashMap<String, List<String>> commands, Logger log) {
        this.host = host;
        this.port = port;
        this.serverNode = serverNode;
        this.sockets = sockets;
        this.commands = commands;
        this.log = log;
        this.clientLogMsg = "[server: " + host + ":"+ port + "] ";
    }

    public void run() {
        log.info(clientLogMsg + "- launched");
        if (!isConnectableAddress(host, port)) {
            log.warning(clientLogMsg + "Node: " + host + ":" + port + "; is not a connectable address!");
            this.close();
        }

        try {
            clientSocket = new Socket(host, port);
            sockets.add(clientSocket);
            clientSocket.setSoTimeout(1000*90); // terminate after 90s
            log.info(clientLogMsg + "- connected to new server");

            // prepare the input reader for the socket
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            // prepare the writer for responding to clients requests
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());

            String request = "";
            String response = "";
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(Transaction.class, new TransactionSerializer());
            objectMapper.registerModule(module);
            objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

            // First send a hello
            HelloMessage firstHello = new HelloMessage(hello, "0.8.0", serverNode.getName() + " " + serverNode.getVersionOfNode());
            response = objectMapper.writeValueAsString(firstHello);
            writer.println(response);
            writer.flush();
            log.info(clientLogMsg + "- sent first hello:" + response);

            // Second send getpeers
            GetPeersMessage firstGetPeers = new GetPeersMessage(getpeers);
            response = objectMapper.writeValueAsString(firstGetPeers);
            writer.println(response);
            writer.flush();
            log.info(clientLogMsg + "- sent first getpeers:" + response);

            boolean handshakeCompleted = false;
            boolean broadcasting = !commands.isEmpty();
            boolean commandsSent = false;

            while (!Thread.currentThread().isInterrupted()
                    && (!handshakeCompleted || broadcasting)
                    && (request = reader.readLine()) != null) {

                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                log.info(clientLogMsg + "- received: " + request);

                request = request.trim();

                // verify json
                boolean isJson = Util.isJson(request);
                if (!isJson || request == null || request.trim().equals("")) {
                    response = objectMapper.writeValueAsString(new ErrorMessage(error, "Did not receive a valid json-message!"));
                    writer.println(response);
                    writer.flush();
                    log.warning(clientLogMsg + "- did not receive a valid json-message!");
                    break;
                }

                // retrieve json
                JsonNode jsonNode = objectMapper.readTree(request);
                JsonNode typeNode = jsonNode.get("type");

                if (typeNode == null) {
                    response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unsupported message type received!"));
                    writer.println(response);
                    writer.flush();
                    log.warning(clientLogMsg + "- unsupported message type received!");
                    break;
                }

                // retrieve type of the json-msg
                String type = typeNode.textValue();
                if (type == null) {
                    response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unsupported message type received!"));
                    writer.println(response);
                    writer.flush();
                    log.warning(clientLogMsg + "- unsupported message type received!");
                    break;
                } else if (type.equals("getchaintip") || type.equals("getmempool")) {
                    // other message-types not yet required
                    continue;
                }

                boolean badRequest = false;
                boolean continueWithoutResponse = false;
                String key = "";
                Object value = null;

                switch (type) {

                    case hello:
                        // { "type" : "hello", "version" : "0.8.0", "agent" : "Kerma−Core Client 0.8" }
                        // { "version" : "0.8.0", "type" : "hello", "agent" : "Kerma−Core Client 0.8" }
                        log.info(clientLogMsg + "- [Case: HELLO]");
                        if (!isParsableInJson(objectMapper, request, HelloMessage.class)) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Hello-Message could not be parsed!"));
                            log.warning(clientLogMsg + "- hello-Message could not be parsed!");
                            break;
                        }
                        HelloMessage receivedHello = objectMapper.readValue(request, HelloMessage.class);
                        if (!receivedHello.verifyHelloMessage()) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Hello-Message failed verification!"));
                            log.warning(clientLogMsg + "- hello-Message failed verification!");
                            break;
                        }
                        wasGreeted = true;
                        continueWithoutResponse = true;
                        break;

                    case getpeers:
                        // { "type" : "getpeers" }
                        log.info(clientLogMsg + "- [Case: GETPEERS]");
                        if (!wasGreeted) {
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unexpected message; 'hello' was expected!"));
                            log.warning(clientLogMsg + "- unexpected message; 'hello' was expected!");
                            badRequest = true;
                            break;
                        }
                        if (!isParsableInJson(objectMapper, request, GetPeersMessage.class)) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "GetPeers-Message could not be parsed!"));
                            log.warning(clientLogMsg + "- getPeers-Message could not be parsed!");
                            break;
                        }
                        GetPeersMessage receivedGetPeers = objectMapper.readValue(request, GetPeersMessage.class);

                        // peers are stored in servernode
                        List<String> knownPeers = new ArrayList(serverNode.getListOfDiscoveredPeers());
                        knownPeers.add(serverNode.getServerAddress()); // add own address into new list
                        PeersMessage responsePeers = new PeersMessage(peers, knownPeers);
                        response = objectMapper.writeValueAsString(responsePeers);
                        break;

                    case peers:
                        // {"type" : "peers", "peers" : ["****.com:18018" ,"138.197.191.170:18018", "[fe80::f03c:91ff:fe2c:5a79]:18018"] }
                        log.info(clientLogMsg + "- [Case: PEERS]");
                        if (!wasGreeted) {
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unexpected message; 'hello' was expected!"));
                            log.warning(clientLogMsg + "- unexpected message; 'hello' was expected!");
                            badRequest = true;
                            break;
                        }
                        if (!isParsableInJson(objectMapper, request, PeersMessage.class)) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Peers-Message could not be parsed!"));
                            log.warning(clientLogMsg + "- peers-Message could not be parsed!");
                            break;
                        }
                        PeersMessage receivedPeers = objectMapper.readValue(request, PeersMessage.class);
                        List<String> validPeers = receivedPeers.verifyPeersMessage();
                        if (validPeers == null) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Peers-Message failed verification!"));
                            log.warning(clientLogMsg + "- peers-Message failed verification!");
                            break;
                        }
                        log.info(clientLogMsg + "- there were " + validPeers.size() + " valid peers");
                        String peersWereUpdated = serverNode.updateListOfDiscoveredPeers(validPeers);
                        if (peersWereUpdated == null) {
                            log.severe(clientLogMsg + "- ERROR - peers could not be read from file!");
                        } else if (peersWereUpdated.equals("")) {
                            log.info(clientLogMsg + "- no peers were updated");
                        } else {
                            log.info(clientLogMsg + "peers were updated : " + peersWereUpdated);
                        }
                        continueWithoutResponse = true;
                        handshakeCompleted = true;
                        log.info(clientLogMsg + "- handshake completed");
                        break;

                    case getobject:
                        // {"\"type\" : \"getobject\" ,\"objectid\":\"0024839ec9632d382486ba7aac7e0bda3b4bda1d4bd79be9ae78e7e1e813ddd8\"}
                        log.info(clientLogMsg + "- [Case: GETOBJECT]");
                        if (!wasGreeted) {
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unexpected message; 'hello' was expected!"));
                            log.warning(clientLogMsg + "- Unexpected message; 'hello' was expected!");
                            badRequest = true;
                            break;
                        }
                        if (!isParsableInJson(objectMapper, request, GetObjectMessage.class)) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "GetObject-Message could not be parsed!"));
                            log.warning(clientLogMsg + "- GetObject-Message could not be parsed!");
                            break;
                        }
                        GetObjectMessage getObjectMessage = objectMapper.readValue(request, GetObjectMessage.class);
                        key = getObjectMessage.getObjectid();
                        value = serverNode.getListOfObjects().get(key);
                        if(value == null) {
                            // don't have the object
                            continueWithoutResponse = true;
                            break;
                        }
                        ObjectMessage sendObjectMessage = new ObjectMessage(object, value);
                        response = objectMapper.writeValueAsString(sendObjectMessage);
                        break;

                    case ihaveobject:
                        // {"\"type\" : \"ihaveobject\" ,\"objectid\":\"0024839ec9632d382486ba7aac7e0bda3b4bda1d4bd79be9ae78e7e1e813ddd8\"}
                        log.info(clientLogMsg + "- [Case: IHAVEOBJECT]");
                        if (!wasGreeted) {
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unexpected message; 'hello' was expected!"));
                            log.warning(clientLogMsg + "- Unexpected message; 'hello' was expected!");
                            badRequest = true;
                            break;
                        }
                        if (!isParsableInJson(objectMapper, request, IHaveObjectMessage.class)) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "IHaveObject-Message could not be parsed!"));
                            log.warning(clientLogMsg + "- IHaveObject-Message could not be parsed!");
                            break;
                        }
                        IHaveObjectMessage iHaveObjectMessage = objectMapper.readValue(request, IHaveObjectMessage.class);
                        key = iHaveObjectMessage.getObjectid();
                        value = serverNode.getListOfObjects().get(key);
                        if(value != null) {
                            // have the object
                            continueWithoutResponse = true;
                            break;
                        }
                        GetObjectMessage sendGetObjectMessage = new GetObjectMessage(getobject, key);
                        response = objectMapper.writeValueAsString(sendGetObjectMessage);
                        break;

                    case object:
                        log.info(clientLogMsg + "- [Case: OBJECT]");
                        if (!wasGreeted) {
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unexpected message; 'hello' was expected!"));
                            log.warning(clientLogMsg + "- Unexpected message; 'hello' was expected!");
                            badRequest = true;
                            break;
                        }
                        if (!isParsableInJson(objectMapper, request, ObjectMessage.class)) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Object-Message could not be parsed!"));
                            log.warning(clientLogMsg + "- Object-Message could not be parsed!");
                            break;
                        }
                        ObjectMessage objectMessage = objectMapper.readValue(request, ObjectMessage.class);
                        value = objectMessage.getObject();
                        if(serverNode.getListOfObjects().containsValue(value)) {
                            // have the object
                            continueWithoutResponse = true;
                            break;
                        }
                        if(!value.verifyObject(serverNode.getListOfObjects())) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Object-Message could not be verified!"));
                            log.warning(clientLogMsg + "- Object-Message could not be verified!");
                            break;
                        }

                        key = computeHash(objectMapper.writeValueAsString(value));
                        if(serverNode.getListOfObjects().containsKey(key)) {
                            // have the object
                            continueWithoutResponse = true;
                            break;
                        }
                        HashMap<String, Object> newObjects = new HashMap<>();
                        newObjects.put(key,value);
                        String objectsWereUpdated = serverNode.appendToObjects(newObjects);
                        if (objectsWereUpdated == null) {
                            log.severe(clientLogMsg + "- ERROR - objects could not be read from file!");
                        } else if (objectsWereUpdated.equals("")) {
                            log.info(clientLogMsg + "- No objects were updated");
                        } else {
                            log.info(clientLogMsg + "- Objects were updated : " + objectsWereUpdated);
                        }
                        HashMap<String, List<String>> cmds = new HashMap<>();
                        List<String> params = new ArrayList<>();
                        params.add(key);
                        cmds.put(ihaveobject, params);
                        // TODO: activate
                         serverNode.getService().execute(new ClientManagerThread(serverNode, serverNode.getService(), sockets, "broadcast", cmds, log));

                        continueWithoutResponse = true;
                        break;

                    case error:
                        // { "type" : "error" , "error" : "some error" }
                        log.warning(clientLogMsg + "- [Case: ERROR]");
                        response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unexpected error received: " + request));
                        badRequest = true;
                        break;

                    default:
                        badRequest = true;
                        break;
                }

                if (badRequest) {
                    log.warning(clientLogMsg + "- BAD REQUEST!");
                    if (response == null || !response.contains(error)) {
                        response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unsupported message type received!"));
                    }
                    writer.println(response);
                    writer.flush();
                    break;
                }

                if (continueWithoutResponse) {
                    log.info(clientLogMsg + "- continuing without response");

                    if(handshakeCompleted && broadcasting && !commandsSent) {
                        log.info(clientLogMsg + "- begin sending commands: " + commands);
                        for (Map.Entry<String, List<String>> cmd: commands.entrySet()) {
                            switch (cmd.getKey()) {
                                case ihaveobject:
                                    response = objectMapper.writeValueAsString(new IHaveObjectMessage(ihaveobject, cmd.getValue().get(0)));
                                    writer.println(response);
                                    writer.flush();
                                    log.info(clientLogMsg + "- send: " + response);
                                    break;
                                default:
                                    break;
                            }
                        }
                        commandsSent = true;
                        log.info(clientLogMsg + "- finished sending commands");
                    }

                    continue;
                }

                writer.println(response);
                writer.flush();
                log.info(clientLogMsg + "- responded: " + response);

                if(handshakeCompleted && broadcasting && !commandsSent) {
                    log.info(clientLogMsg + "- begin sending commands: " + commands);
                    for (Map.Entry<String, List<String>> cmd: commands.entrySet()) {
                        switch (cmd.getKey()) {
                            case ihaveobject:
                                response = objectMapper.writeValueAsString(new IHaveObjectMessage(ihaveobject, cmd.getValue().get(0)));
                                writer.println(response);
                                writer.flush();
                                log.info(clientLogMsg + "- send: " + response);
                                break;
                            default:
                                break;
                        }
                    }
                    commandsSent = true;
                    log.info(clientLogMsg + "- finished sending commands");
                }
            }

            this.close();

        } catch (SocketException e) {
            // when the socket is closed, the I/O methods of the Socket will throw a SocketException
            // almost all SocketException cases indicate that the socket was closed
            log.warning(clientLogMsg + "- socket was closed");

        } catch (SocketTimeoutException e) {
            // when the socket is closed, the I/O methods of the Socket will throw a SocketException
            // almost all SocketException cases indicate that the socket was closed
            log.warning(clientLogMsg + "- socket did not respond in time");

        } catch (IOException e) {
            log.warning(clientLogMsg + "- JSON Exception!");
            throw new UncheckedIOException(e);

        } catch (Exception e) {
            log.severe(clientLogMsg + "- Unknown exception of client-socket!");

        } finally {
            this.close();
        }
    }

    public void close() {
        if (clientSocket != null && !clientSocket.isClosed()) {
            try {
                clientSocket.close();
                log.info(clientLogMsg + "- clientsocket was closed");
            } catch (IOException e) {
                log.warning(clientLogMsg + "- Error while closing client socket: " + e.getMessage());
            }
        }
    }
}
