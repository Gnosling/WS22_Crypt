package Entities;

import Util.TransactionSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.List;
import java.util.Objects;

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

            @Override
            public boolean equals(java.lang.Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Outpoint that = (Outpoint) o;
                return txid == null ? that.txid == null : txid.equals(that.txid)
                        && index == that.index;
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

        @Override
        public boolean equals(java.lang.Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Input that = (Input) o;
            return outpoint == null ? that.outpoint == null : outpoint.equals(that.outpoint)
                    && sig == null ? that.sig == null : sig.equals(that.sig);
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
        @Override
        public boolean equals(java.lang.Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Output that = (Output) o;
            return pubkey == null ? that.pubkey == null : pubkey.equals(that.pubkey)
                    && value == that.value;
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

    public Transaction() {}

    public Transaction(String type, long height, List<Input> inputs, List<Output> outputs) {
        this.type = type;
        this.height = height;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public boolean verifyObject(){
        if(isCoinbase()) {
            return true;
        }
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

    public void setSignatureToNull() {
        for(Input in : inputs) {
            in.setSig(null);
        }
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return type == null ? that.type == null : type.equals(that.type)
                && height == that.height
                && inputs == null ? that.inputs == null : inputs.equals(that.inputs)
                && outputs == null ? that.outputs == null : outputs.equals(that.outputs);
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
        return inputs == null;
    }
}
