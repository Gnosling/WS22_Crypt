import Entities.Block;
import Entities.Object;
import Entities.Transaction;
import Util.Util;
import Util.ContainerOfUTXO;
import Util.TransactionSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static Util.Util.*;

public class ServerNode {

    private int PORT;
    private String IP_ADDRESS;
    private String name;
    private String versionOfNode;
    private List<String> listOfDiscoveredPeers;
    private HashMap<String, Object> listOfObjects;
    private String chaintip;
    private long lengthOfLongestChain = -1;
    private final int maxFetchLimitInMillis = 4400;
    private List<String> mempoolList = new ArrayList<>();
    private HashMap<String, List<ContainerOfUTXO>> mempoolUTXO = new HashMap<>();
    private Logger log;

    private ExecutorService service;
    private ServerSocket serverSocket;
    private List<Socket> sockets = new ArrayList<>();
    private String serverAddress;

    public ServerNode(int PORT, String IP_ADDRESS, String name, String versionOfNode,
                      List<String> listOfDiscoveredPeers, HashMap<String, Object> listOfObjects, Logger log) {
        this.PORT = PORT;
        this.IP_ADDRESS = IP_ADDRESS;
        this.name = name;
        this.versionOfNode = versionOfNode;
        this.listOfDiscoveredPeers = listOfDiscoveredPeers;
        this.listOfObjects = listOfObjects;
        this.log = log;
    }

    public void launch() {
        log.info("launching ...");
        // set chaintip to genesis
        chaintip = hashIdOfGenesisBlock;

        service = Executors.newFixedThreadPool(150);

        try {
            serverSocket = new ServerSocket(PORT);
            this.serverAddress = IP_ADDRESS + ":" + PORT;

            service.execute(new ServerListenerThread(this, serverSocket, service, sockets, log));
            HashMap<String, List<String>> cmds = new HashMap<>();
            cmds.put(getchaintip, null);
            // TODO: don't activate
//            service.execute(new ClientManagerThread(this, service, sockets, "broadcast", cmds, log));
            log.info("launched");
        } catch (IOException e) {
            throw new UncheckedIOException("Error while server socket: ", e);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {
            // read commands from the console
            while (true) {
                String cmd = reader.readLine();

                if (cmd == null) {
                    continue;
                }

                // close sockets and listening threads
                if (("shutdown").equals(cmd)) {
                    this.shutdown();
                    break;

                } else if (("info").equals(cmd)) {
                    log.info(" --- INFO --- "
                            + "\n Server-Address: " + InetAddress.getLocalHost().getHostAddress());

                } else if (("peers").equals(cmd)) {
                    StringBuilder peers = new StringBuilder();
                    for (String peer : listOfDiscoveredPeers) {
                        peers.append(peer + "\n");
                    }
                    log.info(" --- Peers --- \n" + peers);

                } else if (("objects").equals(cmd)) {
                    StringBuilder objects = new StringBuilder();
                    for (Map.Entry<String, Object> entry : listOfObjects.entrySet()) {
                        objects.append(entry.getKey() + ":" + entry.getValue().toString() + "\n");
                    }
                    log.info(" --- Objects --- \n" + objects);


                } else if (("chaintip").equals(cmd)) {
                    log.info(" --- chaintip --- \n" + chaintip + ", with height: " + lengthOfLongestChain);

                } else if (cmd.startsWith("connect to ")) {
                    String[] parts = cmd.split(" ");
                    if (parts.length != 4) {
                        log.warning("bad command; expected 'connect to <host> <port>");
                        continue;
                    }
                    String host = parts[2];
                    String portString = parts[3];
                    int port = 0;
                    try {
                        port = Integer.valueOf(portString);
                    } catch (NumberFormatException numberFormatException) {
                        log.warning("wrong port; expected 'connect to <host> <port>");
                    }
                    HashMap<String, List<String>> commands = new HashMap<>();
                    service.execute(new ClientThread(null, host, port, this, sockets, commands, log));

//                } else if (cmd.equals("start clientmanager")) {
//                    // TODO: start new clientmanager?

                } else if (cmd.equals("reload persistent peers")) {
                    listOfDiscoveredPeers = Util.readPeersOfPersistentFile(Launcher.fileNameOfStoredPeers);
                    log.info("read persistent peers");

                } else {
                    log.warning("unknown command: " + cmd);
                }
            }
        } catch (IOException e) {
            // IOException from System.in is very very unlikely (or impossible)
            // and cannot be handled
        }
    }

    public void shutdown() {
        log.info("shutting down ...");

        if (serverSocket != null) {
            try {
                serverSocket.close();
                log.info("serversocket was closed");
            } catch (IOException e) {
                log.warning("Error while closing server socket: " + e.getMessage());
            }
        }

        for (Socket socket : sockets) {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.severe("Error while closing socket: " + e.getMessage());
                }
            }
        }

        service.shutdown();
        try {
            if (!service.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
        }
        log.info("was shut down!");
    }

    public String getName() {
        return name;
    }

    public String getVersionOfNode() {
        return versionOfNode;
    }

    public List<String> getListOfDiscoveredPeers() {
        return listOfDiscoveredPeers;
    }

    public void setListOfDiscoveredPeers(List<String> listOfDiscoveredPeers) {
        this.listOfDiscoveredPeers = listOfDiscoveredPeers;
    }

    public HashMap<String, Object> getListOfObjects() {
        return listOfObjects;
    }

    public void setListOfObjects(HashMap<String, Object> listOfObjects) {
        this.listOfObjects = listOfObjects;
    }

    public synchronized String updateListOfDiscoveredPeers(List<String> receivedPeers) {

        String updatedInfo = "";
        List<String> knownPeers = Util.readPeersOfPersistentFile(Launcher.fileNameOfStoredPeers);
        if (knownPeers == null) {
            return null;
        }

        for (String peer : receivedPeers) {
            peer = peer.trim();
            if (peer.equals(serverAddress)) {
                continue;
            }
            if (!knownPeers.contains(peer)) {
                knownPeers.add(peer);
                updatedInfo += "new peer: " + peer + ", ";
            }
        }

        if (!updatedInfo.equals("")) {
            listOfDiscoveredPeers = knownPeers;
            Util.storePeersOnPersistentFile(knownPeers, Launcher.fileNameOfStoredPeers);
        }

        return updatedInfo;
    }

    public synchronized String appendToObjects(HashMap<String, Object> receivedObjects) {
        String updatedInfo = "";
        HashMap<String, Object> knownObjects = Util.readObjectsOfPersistentFile(Launcher.fileNameOfStoredObjects, Launcher.fileNameOfStoredUTXOs);
        HashMap<String, Object> newObjects = new HashMap<>();
        if (knownObjects == null) {
            return null;
        }

        for (Map.Entry<String, Object> object : receivedObjects.entrySet()) {
            if (!knownObjects.containsKey(object.getKey())) {
                newObjects.put(object.getKey(), object.getValue());
                listOfObjects.put(object.getKey(), object.getValue());
                updatedInfo += "new object: " + object.getKey() + ", ";
            }
        }

        if (!updatedInfo.equals("")) {
            Util.appendObjectsOnPersistentFile(newObjects, Launcher.fileNameOfStoredObjects);
        }

        return updatedInfo;
    }

    // assumes validation and everything was done beforehand
    public synchronized String appendToUTXOForNewHash(String hashOfBlock, HashMap<String, List<ContainerOfUTXO>> utxo) {

        String updatedInfo = "";
        if (appendUTXOOnPersistentFileForHashOfBlock(Launcher.fileNameOfStoredUTXOs, hashOfBlock, utxo)) {
            updatedInfo = "new UTXO for block " + hashOfBlock;
        }
        return updatedInfo;
    }

    public synchronized String checkAndUpdateChaintip(Block receivedChaintipBlock) {
        if (receivedChaintipBlock == null) {
            return null;
        }
        if (receivedChaintipBlock.getHeight() > lengthOfLongestChain) {
            lengthOfLongestChain = receivedChaintipBlock.getHeight();
            String chaintip = "";
            try {
                chaintip = computeHash(receivedChaintipBlock.toJson());
            } catch (IOException ioException) {
                return null;
            }

            String mempoolMsg = "";

            if (this.chaintip == null) {
                this.chaintip = chaintip;
                mempoolMsg = "chaintip was initialized";
                this.mempoolUTXO = receivedChaintipBlock.getDeepCopyUTXO();
            }

            // case: extend of longest chain by 1
            else if (this.chaintip.equals(receivedChaintipBlock.getPrevid())) {
                this.mempoolUTXO = receivedChaintipBlock.getDeepCopyUTXO();

                for (String txID : receivedChaintipBlock.getTxids()) {
                    mempoolList.remove(txID);
                }
                try {
                    updateMempool();
                    mempoolMsg = "mempool was updated";
                } catch (Exception e) {
                    mempoolMsg = "error during mempool update";
                }
            } else {
                // case: longest change did reorg

                // take chaintip-block and received chaintip to get last common ancestor
                Block oldChaintipBlock = (Block) listOfObjects.get(this.chaintip);
                Block LCA = null;
                Block tempOld = oldChaintipBlock;
                List<Block> oldChain = new ArrayList<>();
                oldChain.add(oldChaintipBlock);
                Block tempNew = receivedChaintipBlock;
                List<Block> newChain = new ArrayList<>();
                newChain.add(receivedChaintipBlock);

                while (tempOld != null) {
                    tempOld = (Block) listOfObjects.get(tempOld.getPrevid());
                    oldChain.add(tempOld);
                }

                while (tempNew != null) {
                    tempNew = (Block) listOfObjects.get(tempNew.getPrevid());
                    newChain.add(tempNew);

                    if (oldChain.contains(tempNew)) {
                        LCA = tempNew;
                        break;
                    }
                }

                if (LCA == null) {
                    mempoolMsg = "error - mempool could not be updated";
                } else {

                    // add tx from old-chain to mempool
                    List<String> freedTxIDs = new ArrayList<>();
                    for (int i = oldChain.indexOf(LCA); i >= 0; i--) { // reversed order to add them correct in mempool
                        List<String> temp = oldChain.get(i).getTxids();
                        if (temp.isEmpty()) { continue; }
                        Transaction potentialCB = (Transaction) listOfObjects.get(temp.get(0));
                        if (potentialCB == null || potentialCB.isCoinbase()) {
                            temp.remove(0);
                        }
                        freedTxIDs.addAll(temp);
                    }
                    mempoolList.addAll(freedTxIDs); // no coinbases

                    // from LCA go forward and remove tx from mempool
                    List<String> removedTxIDs = new ArrayList<>();
                    for (int i = newChain.indexOf(LCA); i >= 0; i--) {
                        removedTxIDs.addAll(newChain.get(i).getTxids());
                    }
                    mempoolList.removeAll(removedTxIDs);

                    // update mempool on UTXO of the new chaintip
                    mempoolUTXO = receivedChaintipBlock.getDeepCopyUTXO();
                    try {
                        updateMempool();
                        mempoolMsg = "mempool was updated";
                    } catch (Exception e) {
                        mempoolMsg = "error during mempool update";
                    }
                }
            }

            this.chaintip = chaintip;
            return "Chain is longer -> chaintip was updated; " + mempoolMsg;
        } else {
            return "Chain is not longer -> No update";
        }
    }

    /**
     * applies all tx in the mempool to the current mempoolUTXO, in given order.
     * removes invalid tx from mempool-list and cleansup mempoolUTXO.
     * must be called after reset of mempoolUTXO.
     *
     * @throws Exception
     */
    public void updateMempool() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Transaction.class, new TransactionSerializer());
        objectMapper.registerModule(module);
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

        List<String> invalidTxIds = new ArrayList<>();
        for (String txID : mempoolList) {

            // check that each transaction is using inputs from UTXO
            // note: first-come first-served of tx
            Transaction tx = (Transaction) listOfObjects.get(txID);

            // validate tx
            if (!tx.verifyObject(listOfObjects)) {
                invalidTxIds.add(txID);
                continue;
            }

            if (!tx.isCoinbase()) {
                // Each input corresponds to unspent entry in the UTXO
                for (Transaction.Input in : tx.getInputs()) {

                    if (invalidTxIds.contains(txID)) {
                        continue;
                    }

                    Transaction.Input.Outpoint elem = in.getOutpoint();
                    if (!mempoolUTXO.containsKey(elem.getTxid())) {
                        invalidTxIds.add(txID);
                        continue;
                    }
                    List<ContainerOfUTXO> conList = mempoolUTXO.get(elem.getTxid());
                    if (conList == null || conList.isEmpty()) {
                        invalidTxIds.add(txID);
                        continue;
                    }

                    boolean correctIndexExists = false;
                    for (ContainerOfUTXO con : conList) {

                        if (invalidTxIds.contains(txID)) {
                            continue;
                        }

                        if (con.getIndex() != elem.getIndex()) {
                            continue;
                        }
                        if (!con.getIsUnspent()) {
                            invalidTxIds.add(txID); // coins were already spent
                            continue;
                        }
                        // update taken UTXO
                        con.setIsUnspent(false);
                        correctIndexExists = true;
                        break;
                    }
                    if (!correctIndexExists) {
                        invalidTxIds.add(txID);
                    }
                }
            }

            if (!(invalidTxIds.contains(txID))) {
                // update mempoolUTXO
                String newTxHash = computeHash(objectMapper.writeValueAsString(tx));
                List<ContainerOfUTXO> newConList = new ArrayList<>();
                for (int i = 0; i < tx.getOutputs().size(); i++) {
                    newConList.add(new ContainerOfUTXO(i, true));
                }
                mempoolUTXO.put(newTxHash, newConList);
            }
        }

        // remove invalid tx
        for (String txID : invalidTxIds) {
            mempoolList.remove(txID);
        }

        // clean up mempoolUTXO
        cleanUpMempoolUTXO();
    }

    /**
     * applies this tx in the mempool to the current mempoolUTXO, if possible
     * removes invalid tx from mempool-list and cleansup mempoolUTXO.
     *
     * @param tx transaction to be updated
     */
    public synchronized String updateMempoolSingle(Transaction tx, String txID) {

        Block temp = new Block();
        temp.setUTXO(mempoolUTXO);
        HashMap<String, List<ContainerOfUTXO>> changedUTXO = temp.getDeepCopyUTXO();

        if (tx == null || tx.isCoinbase() || mempoolList.contains(txID)) {
            return "warning - no update needed";
        }

        // Each input corresponds to unspent entry in the UTXO
        for (Transaction.Input in : tx.getInputs()) {

            Transaction.Input.Outpoint elem = in.getOutpoint();
            if (!changedUTXO.containsKey(elem.getTxid())) {
                return "warning - tx is not mempool-valid";
            }
            List<ContainerOfUTXO> conList = changedUTXO.get(elem.getTxid());
            if (conList == null || conList.isEmpty()) {
                return "warning - tx is not mempool-valid";
            }

            boolean correctIndexExists = false;
            for (ContainerOfUTXO con : conList) {

                if (con.getIndex() != elem.getIndex()) {
                    continue;
                }
                if (!con.getIsUnspent()) {
                    return "warning - tx is not mempool-valid"; // coins were already spent
                }
                // update taken UTXO
                con.setIsUnspent(false);
                correctIndexExists = true;
                break;
            }
            if (!correctIndexExists) {
                return "warning - tx is not mempool-valid";
            }
        }

        // set mempoolUTXO
        mempoolUTXO = changedUTXO;

        // add to mempoollist
        mempoolList.add(txID);

        // clean up mempoolUTXO
        cleanUpMempoolUTXO();
        return "tx was inserted into mempool";
    }

    public void cleanUpMempoolUTXO() {
        List<String> removeKeys = new ArrayList<>();
        for (Map.Entry<String, List<ContainerOfUTXO>> elem : mempoolUTXO.entrySet()) {
            if (elem.getValue().isEmpty()) {
                removeKeys.add(elem.getKey()); // remove if no list exists
                continue;
            }
            List<ContainerOfUTXO> removers = new ArrayList<>();
            for (ContainerOfUTXO con : elem.getValue()) {
                if (!con.getIsUnspent()) {
                    // remove all spent coins
                    removers.add(con);
                }
            }
            for (ContainerOfUTXO rem : removers) {
                elem.getValue().remove(rem);
            }
            if (elem.getValue().isEmpty()) {
                removeKeys.add(elem.getKey()); // remove if no list exists
            }
        }
        for (String key : removeKeys) {
            mempoolUTXO.remove(key);
        }
    }

    public ExecutorService getService() {
        return service;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public List<Socket> getSockets() {
        return sockets;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getChaintip() {
        return chaintip;
    }

    public long getLengthOfLongestChain() {
        return lengthOfLongestChain;
    }

    public List<String> getMempoolList() {
        return mempoolList;
    }

    public HashMap<String, List<ContainerOfUTXO>> getMempoolUTXO() {
        return mempoolUTXO;
    }

    public int getMaxFetchLimitInMillis() {
        return maxFetchLimitInMillis;
    }
}
