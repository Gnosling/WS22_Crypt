package messages;

public class ErrorMessage {

    private String type;
    private String error;

    public ErrorMessage() {};

    public ErrorMessage(String type, String error) {
        this.type = type;
        this.error = error;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
