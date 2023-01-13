package messages;

public class GetMempoolMessage {

    private String type;

    public GetMempoolMessage() {}

    public GetMempoolMessage(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
