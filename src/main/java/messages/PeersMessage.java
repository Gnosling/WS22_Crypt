package messages;

import java.util.List;

public class PeersMessage {

    private String type;
    private List<String> peers;

    public PeersMessage() {}

    public PeersMessage(String type, List<String> peers) {
        this.type = type;
        this.peers = peers;
    }

    // TODO: not sure if this method is useful / needed?
    public boolean verifyPeersMessage() {
        boolean mandatoryExists = type != null && peers != null;
        if (!mandatoryExists) {
            return false;
        }

        if (!type.equals("peers")) {
            return false;
        }

        for (String peer : peers) {
            // TODO: geht nicht so einfach für IP6!!!
            String[] parts = peer.split(":");
            if (parts.length != 2 || parts[0].equals("") || parts[1].equals("")) {
                return false;
            }
        }

        return true;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getPeers() {
        return peers;
    }

    public void setPeers(List<String> peers) {
        this.peers = peers;
    }
}
