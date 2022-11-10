package Entities;

import com.fasterxml.jackson.annotation.JsonProperty;

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
        return false;
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
