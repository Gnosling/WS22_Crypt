package Entities;

import Util.ContainerOfUTXO;
import Util.TransactionSerializer;
import Util.Util;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Util.Util.computeHash;


@JsonIgnoreProperties({"UTXO", "genesis"})
public class Block implements Object {

    private String type;

    private List<String> txids;

    private String nonce;

    private String previd;

    private long created;

    @JsonProperty("T")
    private String t;

    private String miner;
    private String note;

    @JsonIgnore
    private HashMap<String, List<ContainerOfUTXO>> UTXO;

    @JsonIgnore
    private long height;

    public Block() {
    }

    public Block(String type,
                 List<String> txids,
                 String nonce,
                 String previd,
                 long created,
                 String t,
                 String miner,
                 String note) {
        this.type = type;
        this.txids = txids;
        this.nonce = nonce;
        this.previd = previd;
        this.created = created;
        this.t = t;
        this.miner = miner;
        this.note = note;
    }

    public Block(String type, List<String> txids, String nonce, String previd, long created, String t, String miner, String note, HashMap<String, List<ContainerOfUTXO>> UTXO, long height) {
        this.type = type;
        this.txids = txids;
        this.nonce = nonce;
        this.previd = previd;
        this.created = created;
        this.t = t;
        this.miner = miner;
        this.note = note;
        this.UTXO = UTXO;
        this.height = height;
    }

    // fetches unknown transactions of that block
    public List<String> getUnknownTransaction(HashMap<String, Object> listOfKnownObjects) {
        List<String> unknownHashs = new ArrayList<>();
        for (String txID : txids) {
            if (!listOfKnownObjects.containsKey(txID)) {
                unknownHashs.add(txID);
            }
        }
        return unknownHashs;
    }

    public boolean verifyObject(HashMap<String, Object> listOfKnownObjects){
        // TODO: created is integer UNIX timestamp in seconds, later than all its pred but not after currenttime --> not this task

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Transaction.class, new TransactionSerializer());
        objectMapper.registerModule(module);
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

        // case genesis block
        if (isGenesis()) {
            height = 0;
            return true; // genesis is always valid
        }

        // check mandatory fields
        if (type == null || nonce == null || previd == null || created == 0 || t == null || txids == null || txids.isEmpty()) {
            return false;
        }


        // check format of fields
        byte[] nonceAsBytes = BaseEncoding.base16().decode(nonce.toUpperCase());
        if (nonceAsBytes.length != 32) { return false; }
        if (!t.equals("00000002af000000000000000000000000000000000000000000000000000000")) { return false; }
        if (created < 0) { return false; }
        if (miner != null && miner.length() > 128) { return false; }
        if (note != null && note.length() > 128) { return false; }


        // check proof-of-work
        String hash = "";
        try {
            hash = computeHash(this.toJson());
        } catch (IOException ioException) {
            return false;
        }
        if (t.compareTo(hash) <= 0) {
            return false; // hash is greater than t
        }

        // Note: ServerListener handles recursive fetching
        if (!listOfKnownObjects.containsKey(previd)) {
            return false;
        }

        Object obj = listOfKnownObjects.get(previd);
        if (!(obj instanceof Block)) {
            return false;
        }

        // check check created
        long currentTime = Instant.now().getEpochSecond();
        if (((Block) obj).getCreated() > created || created > currentTime) {
            return false;
        }

        // set height
        height = ((Block) obj).height + 1;

        // check transactions of block
        boolean first = true;
        Transaction coinbaseTx = null;
        for (String txID : txids) {

            if (!listOfKnownObjects.containsKey(txID)) {
                return false;
            }

            obj = listOfKnownObjects.get(txID);
            if (!(obj instanceof Transaction)) {
                return false;
            }
            Transaction tx = (Transaction) obj;
            if (!tx.verifyObject(listOfKnownObjects)) {
                return false;
            }

            if (!first && tx.isCoinbase()) {
                return false; // coinbase must be the first entry, if it exists
            }
            if (tx.isCoinbase()) {
                coinbaseTx = tx;
            }
            first = false;
        }

        // check height of coinbaseTx
        if (coinbaseTx.getHeight() != height) {
            return false;
        }


        // check that coinbase is not spent in another transaction in the same block
        // and also that coinbase value is not larger than block reward plus the fees
        if (coinbaseTx != null) {
            String coinbaseHash = "";
            long sumOfFees = 0;
            try {
                coinbaseHash = computeHash(objectMapper.writeValueAsString(coinbaseTx));
            } catch (IOException ioException) {
                return false;
            }
            for (String txID : txids) {
                obj = listOfKnownObjects.get(txID);
                if (!(obj instanceof Transaction)) {
                    return false;
                }
                Transaction tx = (Transaction) obj;
                if (tx.isCoinbase()) {
                    continue;
                }
                sumOfFees += tx.getFeeOfTransaction(listOfKnownObjects);
                if (tx.usesIdAsInput(coinbaseHash)) {
                    return false;
                }
            }

            if (coinbaseTx.getValueOfCoinbase() < 50 * Math.pow(10, 12)
                    || coinbaseTx.getValueOfCoinbase() > (50 * Math.pow(10, 12) + sumOfFees)) {
                return false;
            }
        }


        return true;
    }

    /**
     * validates the UTXO-stuff, must be called after normal validation
     * @param listOfKnownObjects
     * @return
     */
    public boolean updateAndCheckUTXO(HashMap<String, Object> listOfKnownObjects) throws Exception {

        // case: genesis
        if (isGenesis()) {
            UTXO = new HashMap<>();
            return true;
        }

        // initialize UTXO
        Block prev = (Block) listOfKnownObjects.get(previd);
        UTXO = prev.getDeepCopyUTXO();

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Transaction.class, new TransactionSerializer());
        objectMapper.registerModule(module);
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

        // check that each transaction is using inputs from UTXO
        // note: first-come first-served of tx
        for (String txID : txids) {
            Transaction tx  = (Transaction) listOfKnownObjects.get(txID);

            // validate tx
            if (!tx.verifyObject(listOfKnownObjects)) {
                return false;
            }

            if (!tx.isCoinbase()) {
                // Each input corresponds to unspent entry in the UTXO
                for (Transaction.Input in : tx.getInputs()) {
                    Transaction.Input.Outpoint elem = in.getOutpoint();
                    if (!UTXO.containsKey(elem.getTxid())) {
                        return false; // unknown input
                    }
                    List<ContainerOfUTXO> conList = UTXO.get(elem.getTxid());
                    if (conList == null || conList.isEmpty()) {
                        return false;
                    }

                    for (ContainerOfUTXO con : conList) {
                        if (con.getIndex() != elem.getIndex()) {
                            continue;
                        }
                        if (!con.getIsUnspent()) {
                            return false; // coins were already spent
                        }
                        // update taken UTXO
                        con.setIsUnspent(false);
                        break;
                    }
                }
            }

            // update inputted UTXO
            String newTxHash = computeHash(objectMapper.writeValueAsString(tx));
            List<ContainerOfUTXO> newConList = new ArrayList<>();
            for (int i = 0; i < tx.getOutputs().size(); i++) {
                newConList.add(new ContainerOfUTXO(i, true));
            }
            UTXO.put(newTxHash, newConList);
        }

        // clean up UTXO
        List<String> removeKeys = new ArrayList<>();
        for (Map.Entry<String, List<ContainerOfUTXO>> elem : UTXO.entrySet()) {
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
            UTXO.remove(key);
        }

        return true;
    }



    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Transaction.class, new TransactionSerializer());
        objectMapper.registerModule(module);
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

        return objectMapper.writeValueAsString(this);
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block that = (Block) o;
        return type == null ? that.type == null : type.equals(that.type)
                && txids == null ? that.txids == null : txids.equals(that.txids)
                && nonce == null ? that.nonce == null : nonce.equals(that.nonce)
                && previd == null ? that.previd == null : previd.equals(that.previd)
                && created == that.created
                && t == null ? that.t == null : t.equals(that.t)
                && miner == null ? that.miner == null : miner.equals(that.miner)
                && note == null ? that.note == null : note.equals(that.note);
    }

    @JsonIgnore
    public boolean isGenesis() {

        try {
            String json = this.toJson();
            String hash = computeHash(json);
            return Util.hashIdOfGenesisBlock.equals(hash);
        } catch (Exception e) {
            return false;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getTxids() {
        return txids;
    }

    public void setTxids(List<String> txids) {
        this.txids = txids;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getPrevid() {
        return previd;
    }

    public void setPrevid(String previd) {
        this.previd = previd;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getT() {
        return this.t;
    }

    public void setT(String t) {
        this.t = t;
    }

    public String getMiner() {
        return miner;
    }

    public void setMiner(String miner) {
        this.miner = miner;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @JsonIgnore
    public HashMap<String, List<ContainerOfUTXO>> getThisUTXO() {
        return UTXO;
    }

    @JsonIgnore
    public HashMap<String, List<ContainerOfUTXO>> getDeepCopyUTXO() {

        HashMap<String, List<ContainerOfUTXO>> copy = new HashMap<>();
        for (Map.Entry<String, List<ContainerOfUTXO>> entry : UTXO.entrySet()) {
            String hashID = entry.getKey();
            List<ContainerOfUTXO> outputs = entry.getValue();
            List<ContainerOfUTXO> temp = new ArrayList<>();
            for (ContainerOfUTXO elem : outputs) {
                temp.add(new ContainerOfUTXO(elem.getIndex(), elem.getIsUnspent()));
            }
            copy.put(hashID, temp);
        }

        return copy;
    }

    @JsonIgnore
    public void setUTXO(HashMap<String, List<ContainerOfUTXO>> UTXO) {
        this.UTXO = UTXO;
    }
}
