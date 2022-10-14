package messages;

public class GetPeersMessage {

    private String type;

    public GetPeersMessage() {}

    public GetPeersMessage(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
