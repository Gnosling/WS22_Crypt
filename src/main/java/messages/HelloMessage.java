package messages;

public class HelloMessage {
    private String type;
    private String version; // must be 0.8.X
    private String agent; // optional

    public HelloMessage() {}

    public HelloMessage(String type, String version, String agent) {
        this.type = type;
        this.version = version;
        this.agent = agent;
    }

    public boolean verifyHelloMessage() {
        boolean mandatoryExists = type != null && version != null;

        if (!mandatoryExists) {
            return false;
        }

        if (!type.equals("hello")) {
            return false;
        }

        if (!version.startsWith("0.8.")) {
            return false;
        }

        return true;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }
}
