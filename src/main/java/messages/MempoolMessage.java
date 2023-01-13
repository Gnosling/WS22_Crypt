package messages;

import java.util.List;

public class MempoolMessage {

    private String type;

    private List<String> txids;

    public MempoolMessage() {}

    public MempoolMessage(String type, List<String> txids) {
        this.type = type;
        this.txids = txids;
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
}
