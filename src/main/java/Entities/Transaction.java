package Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

//@JsonPropertyOrder({
//        "height",
//        "inputs",
//        "outputs",
//        "type"
//})
public class Transaction implements Object {

    private static class Input {
        private static class Outpoint {
            private long index;
            private String txid;

            public Outpoint() {}

            public Outpoint(String txid, long index) {
                this.txid = txid;
                this.index = index;
            }

            public String getTxid() {
                return txid;
            }

            public void setTxid(String txid) {
                this.txid = txid;
            }

            public long getIndex() {
                return index;
            }

            public void setIndex(long index) {
                this.index = index;
            }
        }

        private Outpoint outpoint;
        private String sig;

        public Input() {}

        public Input(Outpoint outpoint, String sig) {
            this.outpoint = outpoint;
            this.sig = sig;
        }

        public Outpoint getOutpoint() {
            return outpoint;
        }

        public void setOutpoint(Outpoint outpoint) {
            this.outpoint = outpoint;
        }

        public String getSig() {
            return sig;
        }

        public void setSig(String sig) {
            this.sig = sig;
        }
    }

    private static class Output {
        private String pubkey;
        private long value;

        public Output() {}

        public Output(String pubkey, long value) {
            this.pubkey = pubkey;
            this.value = value;
        }

        public String getPubkey() {
            return pubkey;
        }

        public void setPubkey(String pubkey) {
            this.pubkey = pubkey;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }
    }

    private String type;
    private long height;
    private List<Input> inputs;
    private List<Output> outputs;
    @JsonIgnore
    private boolean coinbase;

    public Transaction() {}

    public Transaction(String type, long height, List<Input> inputs, List<Output> outputs) {
        this.type = type;
        this.height = height;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public boolean verifyObject(){
        // TODO: implement
        // index + value is int >= 0
        // sum of inputs must be >= value in output
        return false;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Input> getInputs() {
        return inputs;
    }

    public void setInputs(List<Input> inputs) {
        this.inputs = inputs;
    }

    public List<Output> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<Output> outputs) {
        this.outputs = outputs;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public boolean isCoinbase() {
        if (!coinbase) {
            if (inputs == null) {
                coinbase = true;
            } else {
                coinbase = false;
            }
        }
        return coinbase;
    }

    public void setCoinbase(boolean coinbase) {
        this.coinbase = coinbase;
    }
}
