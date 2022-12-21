package messages;

public class GetChaintipMessage {

    private String type;

    public GetChaintipMessage() {}

    public GetChaintipMessage(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
