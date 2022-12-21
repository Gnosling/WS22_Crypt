import Entities.Block;
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
            socket.setSoTimeout(1000*10); // terminate after 10s

            String host = socket.getInetAddress().getHostAddress();
            int port = socket.getPort();

            serverNode.getListOfDiscoveredPeers().add(host + ":" + port);
            log.info("Connected to new client: " + host + ":" + port);

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

            // Third send getchaintip
            GetChaintipMessage getChaintipMessage = new GetChaintipMessage(getchaintip);
            response = objectMapper.writeValueAsString(getChaintipMessage);
            writer.println(response);
            writer.flush();
            log.info("[first-asked-for-chaintip]: " + response);

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
                } else if (type.equals("getmempool")) {
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

                        if (value instanceof Block) {
                            Block block = (Block) value;
                            boolean errorDuringRecursivePredecessorChecking = false;

                            List<Block> predecessors = new ArrayList<>();
                            Block currentIterationBlock = block;

                            // first fetch all predecessors
                            while (!serverNode.getListOfObjects().containsKey(currentIterationBlock.getPrevid())
                                    && !currentIterationBlock.isGenesis()) {

                                HashMap<String, List<String>> cmds = new HashMap<>();
                                List<String> params = new ArrayList<>();
                                params.add(currentIterationBlock.getPrevid());
                                cmds.put(getobject, params);
                                // TODO: activate
                                ClientManagerThread clientManger = new ClientManagerThread(serverNode, service, sockets, "broadcast", cmds, log);
                                service.execute(clientManger);
                                // or while loop for fetching return values of client manager?
                                try {
                                    Thread.sleep(1000*1); // wait 1 second
                                } catch (Exception e) {

                                }

                                Object predecessor = null;
                                List<Block> foundBlocks = clientManger.getFoundBlocks();

                                for (Block elem : foundBlocks) {
                                    String hash = "";
                                    try {
                                        hash = computeHash(elem.toJson());
                                        if (hash.equals(currentIterationBlock.getPrevid())) {
                                            predecessor = elem;
                                        }
                                    } catch (IOException ioException) { }
                                }

                                if (!(predecessor instanceof Block)) {
                                    errorDuringRecursivePredecessorChecking = true;
                                    break;
                                }
                                currentIterationBlock = (Block) predecessor;
                                predecessors.add(currentIterationBlock);
                            }

                            if (errorDuringRecursivePredecessorChecking) {
                                badRequest = true;
                                response = objectMapper.writeValueAsString(new ErrorMessage(error, "Predecessors of Block could not be fetched on time!"));
                                log.warning("Predecessors of Block could not be fetched on time!");
                                break;
                            }

                            // second validate all predecessors and update UTXO
                            for (int i = predecessors.size()-1; i >= 0; i--) {
                                Block currentPredecessorBlock = predecessors.get(i);

                                // handle missing transactions
                                List<String> unknownTxs = currentPredecessorBlock.getUnknownTransaction(serverNode.getListOfObjects());
                                if (!unknownTxs.isEmpty()) {
                                    HashMap<String, List<String>> cmds = new HashMap<>();
                                    for (String unknownTx : unknownTxs) {
                                        List<String> params = new ArrayList<>();
                                        params.add(unknownTx);
                                        cmds.put(getobject, params);
                                    }
                                    // TODO: activate
                                    service.execute(new ClientManagerThread(serverNode, service, sockets, "broadcast", cmds, log));
                                    try {
                                        Thread.sleep(1000*1); // wait 1 second
                                    } catch (Exception e) { }
                                }

                                // handle verification
                                if (!currentPredecessorBlock.verifyObject(serverNode.getListOfObjects())) {
                                    errorDuringRecursivePredecessorChecking = true;
                                    break;
                                }

                                // handle UTXO
                                try {
                                    boolean UTXOrespected = currentPredecessorBlock.updateAndCheckUTXO(serverNode.getListOfObjects());
                                    if (!UTXOrespected) {
                                        errorDuringRecursivePredecessorChecking = true;
                                        log.warning("Transactions of this block violate the UTXO!");
                                        break;
                                    }
                                    String utxoWasUpdated =  serverNode.appendToUTXOForNewHash(null, currentPredecessorBlock.getThisUTXO());
                                    if (utxoWasUpdated == null || utxoWasUpdated.equals("")) {
                                        // only an error during saving, the correct UTXO will be non-persistently stored in block
                                        log.severe("No utxo was updated");
                                    } else {
                                        log.info(utxoWasUpdated);
                                    }
                                } catch (Exception e) {
                                    errorDuringRecursivePredecessorChecking = true;
                                    log.warning("Transactions of this block violate the UTXO!");
                                    break;
                                }

                                // persist predecessor block
                                key = computeHash(objectMapper.writeValueAsString(currentPredecessorBlock));

                                HashMap<String, Object> newObjects = new HashMap<>();
                                newObjects.put(key,currentPredecessorBlock);
                                String objectsWereUpdated = serverNode.appendToObjects(newObjects);
                                if (objectsWereUpdated == null) {
                                    log.severe("ERROR - objects could not be read from file!");
                                } else if (objectsWereUpdated.equals("")) {
                                    log.info("No objects were updated");
                                } else {
                                    log.info("Objects were updated : " + objectsWereUpdated);
                                }
                            }

                            if (errorDuringRecursivePredecessorChecking) {
                                badRequest = true;
                                response = objectMapper.writeValueAsString(new ErrorMessage(error, "Predecessors of Block failed verification!"));
                                log.warning("Predecessors of Block failed verification!");
                                break;
                            }

                            // handle missing transactions of most recent block
                            List<String> unknownTxs = block.getUnknownTransaction(serverNode.getListOfObjects());
                            if (!unknownTxs.isEmpty()) {
                                HashMap<String, List<String>> cmds = new HashMap<>();
                                for (String unknownTx : unknownTxs) {
                                    List<String> params = new ArrayList<>();
                                    params.add(unknownTx);
                                    cmds.put(getobject, params);
                                }
                                // TODO: activate
                                service.execute(new ClientManagerThread(serverNode, service, sockets, "broadcast", cmds, log));
                                try {
                                    Thread.sleep(1000*1); // wait 1 second
                                } catch (Exception e) {

                                }
                            }
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

                        if (value instanceof Block) {
                            Block block = (Block) value;

                            try {
                                boolean UTXOrespected = block.updateAndCheckUTXO(serverNode.getListOfObjects());
                                if (!UTXOrespected) {
                                    badRequest = true;
                                    response = objectMapper.writeValueAsString(new ErrorMessage(error, "Transactions of this block violate the UTXO!"));
                                    log.warning("Transactions of this block violate the UTXO!");
                                    break;
                                }
                                String utxoWasUpdated =  serverNode.appendToUTXOForNewHash(key, block.getThisUTXO());
                                if (utxoWasUpdated == null || utxoWasUpdated.equals("")) {
                                    // only an error during saving, the correct UTXO will be non-persistently stored in block
                                    log.severe("No utxo was updated");
                                } else {
                                    log.info(utxoWasUpdated);
                                }
                            } catch (Exception e) {
                                badRequest = true;
                                response = objectMapper.writeValueAsString(new ErrorMessage(error, "Transactions of this block violate the UTXO!"));
                                log.warning("Transactions of this block violate the UTXO!");
                                break;
                            }

                            // update chaintip
                            String chaintipWasUpdated = serverNode.checkAndUpdateChaintip(block);
                            if (chaintipWasUpdated == null) {
                                log.severe("ERROR - chaintip could not be updated!");
                            } else {
                                log.info(chaintipWasUpdated);
                            }
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

                    case getchaintip:
                        log.info("[Case: GETCHAINTIP]");
                        if (!wasGreeted) {
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unexpected message; 'hello' was expected!"));
                            log.warning("Unexpected message; 'hello' was expected!");
                            badRequest = true;
                            break;
                        }
                        if (!isParsableInJson(objectMapper, request, GetChaintipMessage.class)) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "GetChaintip-Message could not be parsed!"));
                            log.warning("GetChaintip-Message could not be parsed!");
                            break;
                        }
                        GetChaintipMessage receivedGetChaintip = objectMapper.readValue(request, GetChaintipMessage.class);

                        // chaintip is stored in servernode
                        ChaintipMessage responseChaintip = new ChaintipMessage(chaintip, serverNode.getChaintip());
                        response = objectMapper.writeValueAsString(responseChaintip);
                        break;

                    case chaintip:
                        log.info("[Case: CHAINTIP]");
                        if (!wasGreeted) {
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Unexpected message; 'hello' was expected!"));
                            log.warning("Unexpected message; 'hello' was expected!");
                            badRequest = true;
                            break;
                        }
                        if (!isParsableInJson(objectMapper, request, ChaintipMessage.class)) {
                            badRequest = true;
                            response = objectMapper.writeValueAsString(new ErrorMessage(error, "Chaintip-Message could not be parsed!"));
                            log.warning("Chaintip-Message could not be parsed!");
                            break;
                        }

                        ChaintipMessage receivedChaintip = objectMapper.readValue(request, ChaintipMessage.class);

                        if (!serverNode.getListOfObjects().containsKey(receivedChaintip.getBlockid())) {
                            // fetch Block
                            cmds = new HashMap<>();
                            params = new ArrayList<>();
                            params.add(receivedChaintip.getBlockid());
                            cmds.put(getobject, params);
                            // TODO: activate
                            ClientManagerThread clientManager = new ClientManagerThread(serverNode, service, sockets, "broadcast", cmds, log);
                            service.execute(clientManager);
                            try {
                                Thread.sleep(1000 * 1); // wait 1 second
                            } catch (Exception e) {

                            }

                            List<Block> foundBlocks = clientManager.getFoundBlocks();
                            for (Block elem : foundBlocks) {
                                String hash = "";
                                try {
                                    hash = computeHash(elem.toJson());
                                    if (hash.equals(receivedChaintip.getBlockid())) {
                                        value = elem;
                                    }
                                } catch (IOException ioException) {
                                }
                            }


                            if (!(value instanceof Block)) {
                                badRequest = true;
                                response = objectMapper.writeValueAsString(new ErrorMessage(error, "Chaintip was not fetched on time!"));
                                log.warning("Chaintip was not fetched on time!");
                                break;
                            }

                            // repeat 4A
                            Block block = (Block) value;
                            boolean errorDuringRecursivePredecessorChecking = false;

                            List<Block> predecessors = new ArrayList<>();
                            Block currentIterationBlock = block;

                            // first fetch all predecessors
                            while (!serverNode.getListOfObjects().containsKey(currentIterationBlock.getPrevid())
                                    && !currentIterationBlock.isGenesis()) {

                                cmds = new HashMap<>();
                                params = new ArrayList<>();
                                params.add(currentIterationBlock.getPrevid());
                                cmds.put(getobject, params);
                                // TODO: activate
                                ClientManagerThread clientManger = new ClientManagerThread(serverNode, service, sockets, "broadcast", cmds, log);
                                service.execute(clientManger);
                                try {
                                    Thread.sleep(1000 * 1); // wait 1 second
                                } catch (Exception e) {

                                }

                                Object predecessor = null;
                                foundBlocks = clientManger.getFoundBlocks();

                                for (Block elem : foundBlocks) {
                                    String hash = "";
                                    try {
                                        hash = computeHash(elem.toJson());
                                        if (hash.equals(currentIterationBlock.getPrevid())) {
                                            predecessor = elem;
                                        }
                                    } catch (IOException ioException) {
                                    }
                                }

                                if (!(predecessor instanceof Block)) {
                                    errorDuringRecursivePredecessorChecking = true;
                                    break;
                                }
                                currentIterationBlock = (Block) predecessor;
                                predecessors.add(currentIterationBlock);
                            }

                            if (errorDuringRecursivePredecessorChecking) {
                                badRequest = true;
                                response = objectMapper.writeValueAsString(new ErrorMessage(error, "Predecessors of Block could not be fetched on time!"));
                                log.warning("Predecessors of Block could not be fetched on time!");
                                break;
                            }

                            // second validate all predecessors and update UTXO
                            for (int i = predecessors.size() - 1; i >= 0; i--) {
                                Block currentPredecessorBlock = predecessors.get(i);

                                // handle missing transactions
                                List<String> unknownTxs = currentPredecessorBlock.getUnknownTransaction(serverNode.getListOfObjects());
                                if (!unknownTxs.isEmpty()) {
                                    cmds = new HashMap<>();
                                    for (String unknownTx : unknownTxs) {
                                        params = new ArrayList<>();
                                        params.add(unknownTx);
                                        cmds.put(getobject, params);
                                    }
                                    // TODO: activate
                                    service.execute(new ClientManagerThread(serverNode, service, sockets, "broadcast", cmds, log));
                                    try {
                                        Thread.sleep(1000 * 1); // wait 1 second
                                    } catch (Exception e) {
                                    }
                                }

                                // handle verification
                                if (!currentPredecessorBlock.verifyObject(serverNode.getListOfObjects())) {
                                    errorDuringRecursivePredecessorChecking = true;
                                    break;
                                }

                                // handle UTXO
                                try {
                                    boolean UTXOrespected = currentPredecessorBlock.updateAndCheckUTXO(serverNode.getListOfObjects());
                                    if (!UTXOrespected) {
                                        errorDuringRecursivePredecessorChecking = true;
                                        log.warning("Transactions of this block violate the UTXO!");
                                        break;
                                    }
                                    String utxoWasUpdated = serverNode.appendToUTXOForNewHash(null, currentPredecessorBlock.getThisUTXO());
                                    if (utxoWasUpdated == null || utxoWasUpdated.equals("")) {
                                        // only an error during saving, the correct UTXO will be non-persistently stored in block
                                        log.severe("No utxo was updated");
                                    } else {
                                        log.info(utxoWasUpdated);
                                    }
                                } catch (Exception e) {
                                    errorDuringRecursivePredecessorChecking = true;
                                    log.warning("Transactions of this block violate the UTXO!");
                                    break;
                                }

                                // persist predecessor block
                                key = computeHash(objectMapper.writeValueAsString(currentPredecessorBlock));

                                newObjects = new HashMap<>();
                                newObjects.put(key, currentPredecessorBlock);
                                objectsWereUpdated = serverNode.appendToObjects(newObjects);
                                if (objectsWereUpdated == null) {
                                    log.severe("ERROR - objects could not be read from file!");
                                } else if (objectsWereUpdated.equals("")) {
                                    log.info("No objects were updated");
                                } else {
                                    log.info("Objects were updated : " + objectsWereUpdated);
                                }
                            }

                            if (errorDuringRecursivePredecessorChecking) {
                                badRequest = true;
                                response = objectMapper.writeValueAsString(new ErrorMessage(error, "Predecessors of Block failed verification!"));
                                log.warning("Predecessors of Block failed verification!");
                                break;
                            }

                            // handle missing transactions of most recent block
                            List<String> unknownTxs = block.getUnknownTransaction(serverNode.getListOfObjects());
                            if (!unknownTxs.isEmpty()) {
                                cmds = new HashMap<>();
                                for (String unknownTx : unknownTxs) {
                                    params = new ArrayList<>();
                                    params.add(unknownTx);
                                    cmds.put(getobject, params);
                                }
                                // TODO: activate
                                service.execute(new ClientManagerThread(serverNode, service, sockets, "broadcast", cmds, log));
                                try {
                                    Thread.sleep(1000 * 1); // wait 1 second
                                } catch (Exception e) {

                                }
                            }

                            if (!block.verifyObject(serverNode.getListOfObjects())) {
                                badRequest = true;
                                response = objectMapper.writeValueAsString(new ErrorMessage(error, "Block-Message could not be verified!"));
                                log.warning("Block-Message could not be verified!");
                                break;
                            }
                            key = computeHash(objectMapper.writeValueAsString(block));

                            try {
                                boolean UTXOrespected = block.updateAndCheckUTXO(serverNode.getListOfObjects());
                                if (!UTXOrespected) {
                                    badRequest = true;
                                    response = objectMapper.writeValueAsString(new ErrorMessage(error, "Transactions of this block violate the UTXO!"));
                                    log.warning("Transactions of this block violate the UTXO!");
                                    break;
                                }
                                String utxoWasUpdated = serverNode.appendToUTXOForNewHash(key, block.getThisUTXO());
                                if (utxoWasUpdated == null || utxoWasUpdated.equals("")) {
                                    // only an error during saving, the correct UTXO will be non-persistently stored in block
                                    log.severe("No utxo was updated");
                                } else {
                                    log.info(utxoWasUpdated);
                                }
                            } catch (Exception e) {
                                badRequest = true;
                                response = objectMapper.writeValueAsString(new ErrorMessage(error, "Transactions of this block violate the UTXO!"));
                                log.warning("Transactions of this block violate the UTXO!");
                                break;
                            }


                            newObjects = new HashMap<>();
                            newObjects.put(key, value);
                            objectsWereUpdated = serverNode.appendToObjects(newObjects);
                            if (objectsWereUpdated == null) {
                                log.severe("ERROR - objects could not be read from file!");
                            } else if (objectsWereUpdated.equals("")) {
                                log.info("No objects were updated");
                            } else {
                                log.info("Objects were updated : " + objectsWereUpdated);
                            }
                            cmds = new HashMap<>();
                            params = new ArrayList<>();
                            params.add(key);
                            cmds.put(ihaveobject, params);
                            // TODO: activate
                            service.execute(new ClientManagerThread(serverNode, service, sockets, "broadcast", cmds, log));

                        }

                        // update chaintip
                        String chaintipWasUpdated = serverNode.checkAndUpdateChaintip(((Block)serverNode.getListOfObjects().get(receivedChaintip.getBlockid())));
                        if (chaintipWasUpdated == null) {
                            log.severe("ERROR - chaintip could not be updated!");
                        } else {
                            log.info(chaintipWasUpdated);
                        }
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
                    log.info("[responded]: " + response);
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
