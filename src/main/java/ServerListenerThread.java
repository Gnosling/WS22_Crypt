import Entities.Object;
import Entities.Transaction;
import Util.TransactionSerializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import messages.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import static Util.Util.*;

public class ServerListenerThread extends Thread {

    private ServerNode serverNode;
    private ServerSocket serverSocket;
    private ExecutorService service;
    private List<Socket> sockets;
    private Logger log;

    private boolean wasGreeted = false;

    public ServerListenerThread(ServerNode serverNode, ServerSocket serverSocket, ExecutorService service, List<Socket> sockets, Logger log) {
        this.serverNode = serverNode;
        this.serverSocket = serverSocket;
        this.service = service;
        this.sockets = sockets;
        this.log = log;
    }

    public void run() {
        Socket socket = null;
        boolean connectionOK = false;

        try {
            // wait for Client to connect
            socket = serverSocket.accept();
            sockets.add(socket);
            service.execute(new ServerListenerThread(serverNode, serverSocket, service, sockets, log));
            socket.setSoTimeout(1000*90); // terminate after 90s
            log.info("Connected to new client: " + socket.getInetAddress());

            // prepare the input reader for the socket
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // prepare the writer for responding to clients requests
            PrintWriter writer = new PrintWriter(socket.getOutputStream());

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
            log.info("[first-greeted]: " + response);

            // Second send getpeers
            GetPeersMessage firstGetPeers = new GetPeersMessage(getpeers);
            response = objectMapper.writeValueAsString(firstGetPeers);
            writer.println(response);
            writer.flush();
            log.info("[first-asked-for-peers]: " + response);

            response = "";

            // read client requests
            while (!Thread.currentThread().isInterrupted() && (request = reader.readLine()) != null) {

                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                log.info("[received]: " + request);

                request = request.trim();

                // verify json
                boolean isJson = Util.Util.isJson(request);
                if (!isJson || request == null || request.trim().equals("")) {
                    response = objectMapper.writeValueAsString(new ErrorMessage(error, "Did not receive a valid json-message!"));
                    writer.println(response);
                    writer.flush();
                    log.warning("Did not receive a valid json-message!");
                    break;
                }

                // retrieve json
                JsonNode jsonNode = objectMapper.readTree(request);
                JsonNode typeNode = jsonNode.get("type");

                if (typeNode == null) {
                    response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unsupported message type received!"));
                    writer.println(response);
                    writer.flush();
                    log.warning("Unsupported message type received!");
                    break;
                }

                // retrieve type of the json-msg
                String type = typeNode.textValue();
                if (type == null) {
                    response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unsupported message type received!"));
                    writer.println(response);
                    writer.flush();
                    log.warning("Unsupported message type received!");
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
                        log.info("[Case: HELLO]");
                        if (!isParsableInJson(objectMapper, request, HelloMessage.class)) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Hello-Message could not be parsed!"));
                            log.warning("Hello-Message could not be parsed!");
                            break;
                        }
                        HelloMessage receivedHello = objectMapper.readValue(request, HelloMessage.class);
                        if (!receivedHello.verifyHelloMessage()) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Hello-Message failed verification!"));
                            log.warning("Hello-Message failed verification!");
                            break;
                        }
                        wasGreeted = true;
                        continueWithoutResponse = true;
                        break;

                    case getpeers:
                        // { "type" : "getpeers" }
                        log.info("[Case: GETPEERS]");
                        if (!wasGreeted) {
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unexpected message; 'hello' was expected!"));
                            log.warning("Unexpected message; 'hello' was expected!");
                            badRequest = true;
                            break;
                        }
                        if (!isParsableInJson(objectMapper, request, GetPeersMessage.class)) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "GetPeers-Message could not be parsed!"));
                            log.warning("GetPeers-Message could not be parsed!");
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
                        log.info("[Case: PEERS]");
                        if (!wasGreeted) {
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unexpected message; 'hello' was expected!"));
                            log.warning("Unexpected message; 'hello' was expected!");
                            badRequest = true;
                            break;
                        }
                        if (!isParsableInJson(objectMapper, request, PeersMessage.class)) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Peers-Message could not be parsed!"));
                            log.warning("Peers-Message could not be parsed!");
                            break;
                        }
                        PeersMessage receivedPeers = objectMapper.readValue(request, PeersMessage.class);
                        List<String> validPeers = receivedPeers.verifyPeersMessage();
                        if (validPeers == null) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Peers-Message failed verification!"));
                            log.warning("Peers-Message failed verification!");
                            break;
                        }
                        log.info("There were " + validPeers.size() + " valid peers");
                        String peersWereUpdated = serverNode.updateListOfDiscoveredPeers(validPeers);
                        if (peersWereUpdated == null) {
                            log.severe("ERROR - peers could not be read from file!");
                        } else if (peersWereUpdated.equals("")) {
                            log.info("No peers were updated");
                        } else {
                            log.info("Peers were updated : " + peersWereUpdated);
                        }
                        continueWithoutResponse = true;
                        break;

                    case getobject:
                        // {"\"type\" : \"getobject\" ,\"objectid\":\"0024839ec9632d382486ba7aac7e0bda3b4bda1d4bd79be9ae78e7e1e813ddd8\"}
                        log.info("[Case: GETOBJECT]");
                        if (!wasGreeted) {
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unexpected message; 'hello' was expected!"));
                            log.warning("Unexpected message; 'hello' was expected!");
                            badRequest = true;
                            break;
                        }
                        if (!isParsableInJson(objectMapper, request, GetObjectMessage.class)) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "GetObject-Message could not be parsed!"));
                            log.warning("GetObject-Message could not be parsed!");
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
                        log.info("[Case: IHAVEOBJECT]");
                        if (!wasGreeted) {
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unexpected message; 'hello' was expected!"));
                            log.warning("Unexpected message; 'hello' was expected!");
                            badRequest = true;
                            break;
                        }
                        if (!isParsableInJson(objectMapper, request, IHaveObjectMessage.class)) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "IHaveObject-Message could not be parsed!"));
                            log.warning("IHaveObject-Message could not be parsed!");
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
                        log.info("[Case: OBJECT]");
                        if (!wasGreeted) {
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unexpected message; 'hello' was expected!"));
                            log.warning("Unexpected message; 'hello' was expected!");
                            badRequest = true;
                            break;
                        }
                        if (!isParsableInJson(objectMapper, request, ObjectMessage.class)) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Object-Message could not be parsed!"));
                            log.warning("Object-Message could not be parsed!");
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
                            log.warning("Object-Message could not be verified!");
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
                            log.severe("ERROR - objects could not be read from file!");
                        } else if (objectsWereUpdated.equals("")) {
                            log.info("No objects were updated");
                        } else {
                            log.info("Objects were updated : " + objectsWereUpdated);
                        }
                        HashMap<String, List<String>> cmds = new HashMap<>();
                        List<String> params = new ArrayList<>();
                        params.add(key);
                        cmds.put(ihaveobject, params);
                        // TODO: activate
                        service.execute(new ClientManagerThread(serverNode, service, sockets, "broadcast", cmds, log));

                        continueWithoutResponse = true;
                        break;

                    case error:
                        // { "type" : "error" , "error" : "some error" }
                        log.warning("[Case: ERROR]");
                        response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unexpected error received: " + request));
                        badRequest = true;
                        break;

                    default:
                        badRequest = true;
                        break;
                }

                if (badRequest) {
                    log.warning("BAD REQUEST!");
                    if (response == null || !response.contains(error)) {
                        response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unsupported message type received!"));
                    }
                    writer.println(response);
                    writer.flush();
                    break;
                }

                if (continueWithoutResponse) {
                    log.info("continuing without response");
                    continue;
                }

                writer.println(response);
                writer.flush();
                log.info("[responded]: " + response);
            }

            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                    log.info("socket was closed");
                } catch (IOException e) {
                    // Ignored because we cannot handle it
                }
            }

        } catch (SocketException e) {
            // when the socket is closed, the I/O methods of the Socket will throw a SocketException
            // almost all SocketException cases indicate that the socket was closed
            log.warning("socket was closed ba client");

        } catch (SocketTimeoutException e) {
            // when the socket is closed, the I/O methods of the Socket will throw a SocketException
            // almost all SocketException cases indicate that the socket was closed
            log.warning("socket did not respond in time");

        } catch (IOException e) {
            log.warning("JSON Exception!");
            throw new UncheckedIOException(e);

        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                    log.warning("socket was closed by server");
                } catch (IOException e) {
                    // Ignored because we cannot handle it
                }
            }
        }
    }
}
