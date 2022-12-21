package Entities;

import Util.TransactionSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.io.BaseEncoding;
import org.bouncycastle.crypto.CryptoException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

//@JsonPropertyOrder({
//        "height",
//        "inputs",
//        "outputs",
//        "type"
//})
public class Transaction implements Object {

    public static class Input {
        public static class Outpoint {
            private int index;
            private String txid;

            public Outpoint() {}

            public Outpoint(String txid, int index) {
                this.txid = txid;
                this.index = index;
            }

            @Override
            public boolean equals(java.lang.Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Outpoint that = (Outpoint) o;
                return (txid == null ? that.txid == null : txid.equals(that.txid))
                        && (index == that.index);
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

            public void setIndex(int index) {
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
            return (outpoint == null ? that.outpoint == null : outpoint.equals(that.outpoint))
                    && (sig == null ? that.sig == null : sig.equals(that.sig));
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

    public static class Output {
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
            return (pubkey == null ? that.pubkey == null : pubkey.equals(that.pubkey))
                    && (value == that.value);
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

    public boolean verifyObject(HashMap<String, Object> listOfKnownObjects){
        if(isCoinbase()) {

            // check mandatory fields
            if (inputs != null || outputs == null || height < 0) {
                return false;
            }

            // check output
            if (outputs.size() != 1) { return false; }
            Output out = outputs.get(0);
            byte[] pubBytes = BaseEncoding.base16().decode(out.pubkey.toUpperCase());
            if (pubBytes.length != 32) {
                return false;
            }
            if (out.value < 50 * Math.pow(10,12)) {
                return false; // this is the lower bound, upper bound will be checked by block validation
            }

            // otw. everything fine
            return true;


        } else {

            long inputValue = 0;
            long outputValue = 0;

            // check mandatory fields
            if (type == null || inputs == null || outputs == null) {
                return false;
            }
            if (inputs.size() == 0 || outputs.size() == 0) {
                return false;
            }
            for (Input in : inputs) {
                if (in.outpoint == null || in.sig == null || in.outpoint.txid == null) {
                    return false;
                }
            }
            for (Output out : outputs) {
                if (out.pubkey == null) {
                    return false;
                }
            }

            // check value
            // check pubKey
            for (Output out : this.outputs) {
                try {
                    byte[] pubBytes = BaseEncoding.base16().decode(out.pubkey.toUpperCase());
                    if (pubBytes.length != 32) {
                        return false;
                    }
                    if (out.value < 0) {
                        return false;
                    }
                    outputValue += out.value;
                } catch (IllegalArgumentException | NullPointerException e) {
                    // pubkey is wrong
                    return false;
                }
            }

            // check outpoint
            // check signature
            for (Input in : this.inputs) {
                Object prevObject = listOfKnownObjects.get(in.outpoint.txid);
                if (prevObject == null || !(prevObject instanceof Transaction)) {
                    // don't have the object
                    return false;
                }
                if (!(prevObject instanceof Transaction)) {
                    return false;
                }

                Transaction prevTx = (Transaction) prevObject;
                if (in.outpoint.index >= (prevTx.outputs.size())) {
                    // index outside outputs
                    return false;
                }

                Transaction signableTx = this.clone();
                signableTx.setSignatureToNull();
                String signableString = "";
                try {
                    signableString = signableTx.toJson();
                } catch (JsonProcessingException e) {
                    return false;
                }

                boolean verifiedSignature = false;
                try {
                    verifiedSignature = Util.Util.verifySignature(prevTx.outputs.get(in.outpoint.index).pubkey, signableString, in.sig);
                } catch (CryptoException | NullPointerException e) {
                    return false;
                }
                if (!verifiedSignature) {
                    // signature failed
                    return false;
                }
                inputValue += prevTx.outputs.get(in.outpoint.index).value;
            }

            // check conservation
            if (inputValue < outputValue) {
                return false;
            }

            // otw. everything fine
            return true;
        }
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
        boolean one = type == null ? that.type == null : type.equals(that.type);
        boolean two = height == that.height;
        boolean three = inputs == null ? that.inputs == null : inputs.equals(that.inputs);
        boolean four = outputs == null ? that.outputs == null : outputs.equals(that.outputs);
        return one && three && two && four;
//        return type == null ? that.type == null : type.equals(that.type)
//                && height == that.height
//                && inputs == null ? that.inputs == null : inputs.equals(that.inputs)
//                && outputs == null ? that.outputs == null : outputs.equals(that.outputs);
    }

    @Override
    public Transaction clone() {
        Transaction newTx = new Transaction();
        newTx.setType(type);
        newTx.setHeight(height);
        List<Input> newInputs = new ArrayList<>();
        for (Input in : inputs) {
            newInputs.add(new Input(new Input.Outpoint(in.outpoint.txid, in.outpoint.index), in.sig));
        }
        newTx.setInputs(newInputs);
        List<Output> newOutputs = new ArrayList<>();
        for (Output out : outputs) {
            newOutputs.add(new Output(out.pubkey, out.value));
        }
        newTx.setOutputs(newOutputs);
        return newTx;
    }

    public boolean usesIdAsInput(String id) {
        for (Input in : inputs) {
            if (in.outpoint.txid.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The fee of a transaction is the sum of its input values minus the sum of its output values
     * @return fee if all ok, returns -1 if there was an error (tx should be validated beforehand)
     */
    public long getFeeOfTransaction(HashMap<String, Object> listOfKnownObjects) {

        if (isCoinbase()) {
            return -1;
        }

        long sumOfInputValues = 0;
        for (Input in : this.inputs) {
            Object prevObject = listOfKnownObjects.get(in.outpoint.txid);
            if (prevObject == null || !(prevObject instanceof Transaction)) {
                // don't have the object
                return -1;
            }

            Transaction prevTx = (Transaction) prevObject;
            if (in.outpoint.index >= (prevTx.outputs.size())) {
                // index outside outputs
                return -1;
            }

            sumOfInputValues += prevTx.outputs.get(in.outpoint.index).getValue();
        }

        long sumOfOutputValues = 0;
        for (Output out: outputs) {
            sumOfOutputValues += out.getValue();
        }

        if (sumOfOutputValues > sumOfInputValues) {
            return -1;
        }

        return sumOfInputValues - sumOfOutputValues;
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
        return (inputs == null);
    }

    public long getValueOfCoinbase() {
        if (!isCoinbase()) {
            return -1;
        }
        return outputs.get(0).getValue();
    }
}
