package messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class ObjectMessage {

    private class Subobject{

        private String type;

        private List<String> txids;

        private String nonce;

        private String previd;

        private String created;

        @JsonProperty("T")
        private String t;

        public Subobject() {}

        public Subobject(String type, List<String> txids, String nonce, String previd, String created, String t) {
            this.type = type;
            this.txids = txids;
            this.nonce = nonce;
            this.previd = previd;
            this.created = created;
            this.t = t;
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

        public String getCreated() {
            return created;
        }

        public void setCreated(String created) {
            this.created = created;
        }

        public String getT() {
            return this.t;
        }

        public void setT(String t) {
            this.t = t;
        }
    }

    private String type;

    private Subobject object;

    public ObjectMessage() {}

    public ObjectMessage(String type, Subobject object) {
        this.type = type;
        this.object = object;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Subobject getObject() {
        return object;
    }

    public void setObject(Subobject object) {
        this.object = object;
    }

    public static void main(String[] args) throws Exception{
        ObjectMapper objectMapper = new ObjectMapper();
        String request = "{\n" +
                "\"type\" : \"object\" ,\n" +
                "\"object\" : {\n" +
                "\"type\" : \"block\" ,\n" +
                "\"txids\" : [\"740bcfb434c89abe57bb2bc80290cd5495e87ebf8cd0dadb076bc50453590104\"],\n" +
                "\"nonce\" : \"a26d92800cf58e88a5ecf37156c031a4147c2128beeaf1cca2785c93242a4c8b\" ,\n" +
                "\"previd\" : \"0024839ec9632d382486ba7aac7e0bda3b4bda1d4bd79be9ae78e7e1e813ddd8\" ,\n" +
                "\"created\" : \"1622825642\",\n" +
                "\"T\" : \"003a000000000000000000000000000000000000000000000000000000000000\"\n" +
                "}\n" +
                "}";
        ObjectMessage received = objectMapper.readValue(request, ObjectMessage.class);
        int i = 0;
    }
}
