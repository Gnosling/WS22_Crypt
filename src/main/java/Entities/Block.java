package Entities;

import Util.TransactionSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import static Util.Util.computeHash;


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
    private String UTXO; // TODO: store for txHash for each output how many coins are left, HashMap <hash, <out-index, unspent-coins>>;
    // TODO: 1. check all TXs to take value less then unspent (for each input take id and index), otw invalid
    // TODO: 2. if everything ok, then remove 0 unspents and add outputs of all txs of the block

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

    public boolean verifyObject(HashMap<String, Object> listOfKnownObjects){
        // TODO: implement
        // TODO: [X] miner and note are optinal  and <= 128 chars
        // TODO: [X] nonce is 32-bytes (like hash?)
        // TODO: created is integer UNIX timestamp in seconds, later than all its pred but not after currenttime
        // TODO: [X] T is 32-byte hex integer and always the same
        // TODO: [X] block hash < T
        // TODO: only genesis has null als previd, others need hash of pred
        // TODO: [X] if coinbase tx, dann muss es erste sein in txids und sein height muss die höhe des blocks sein? --> genesis has height 0, it's the index of the chain! --> height nicht part von dem teil!
        // TODO: exclude genesis block!
        // TODO: after good verification update transactions into UTFX!

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Transaction.class, new TransactionSerializer());
        objectMapper.registerModule(module);

        // case genesis block
        // TODO: implement

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
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        String hash = "";
        try {
            hash = computeHash(objectMapper.writeValueAsString(this));
        } catch (IOException ioException) {
            return false;
        }
        if (t.compareTo(hash) <= 0) {
            return false; // hash is greater than t
        }


        // check chain of block (and get block height; and check created) TODO: not yet in this task?
        //      long currentTime = Instant.now().getEpochSecond()


        // check transactions of block
        boolean first = true;
        Transaction coinbaseTx = null;
        for (String txID : txids) {

            if (!listOfKnownObjects.containsKey(txID)) {
                // TODO: If not known, then send a "getobject" message to your peers in order to get the transaction!!
                return false;
            }

            Object obj = listOfKnownObjects.get(txID);
            if (!obj.getClass().getName().equals("Entities.Transaction")) {
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


        // check that coinbase is not spent in another transaction in the same block
        String coinbaseHash = "";
        try {
            coinbaseHash = computeHash(objectMapper.writeValueAsString(coinbaseTx));
        } catch (IOException ioException) {
            return false;
        }
        for (String txID : txids) {
            Object obj = listOfKnownObjects.get(txID);
            if (!obj.getClass().getName().equals("Entities.Transaction")) {
                return false;
            }
            Transaction tx = (Transaction) obj;
            if (tx.isCoinbase()) {
                continue;
            }
            if (tx.usesIdAsInput(coinbaseHash)) {
                return false;
            }
        }

        return true;
    }

    public boolean updateAndCheckUTXO(HashMap<String, Object> listOfKnownObjects) {

        // initialize UTXO


        // check that each transaction is using inputs from UTXO

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

    public String getUTXO() {
        return UTXO;
    }

    public void setUTXO(String UTXO) {
        this.UTXO = UTXO;
    }
}
