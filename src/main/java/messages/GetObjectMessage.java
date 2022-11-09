package messages;

public class GetObjectMessage {

    private String type;

    private String objectid;

    public GetObjectMessage() {}

    public GetObjectMessage(String type, String objectid) {
        this.type = type;
        this.objectid = objectid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getObjectid() {
        return objectid;
    }

    public void setObjectid(String objectid) {
        this.objectid = objectid;
    }
}
