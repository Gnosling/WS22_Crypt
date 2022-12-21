package messages;

public class ChaintipMessage {

    private String type;

    private String blockid;

    public ChaintipMessage() {}

    public ChaintipMessage(String type, String blockid) {
        this.type = type;
        this.blockid = blockid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBlockid() {
        return blockid;
    }

    public void setBlockid(String blockid) {
        this.blockid = blockid;
    }
}


