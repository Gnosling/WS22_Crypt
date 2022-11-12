package Entities;

import Util.TransactionSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.List;


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

    public Block() {
    }

    public Block(String type,
                 List<String> txids,
                 String nonce,
                 String previd,
                 int created,
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

    public boolean verifyObject(){
        // TODO: implement
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
}
