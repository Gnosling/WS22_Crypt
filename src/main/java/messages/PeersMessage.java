package messages;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PeersMessage {

    private String type;
    private List<String> peers;

    public PeersMessage() {}

    public PeersMessage(String type, List<String> peers) {
        this.type = type;
        this.peers = peers;
    }

    public List<String> verifyPeersMessage() {
        List<String> validPeers = new ArrayList<>();

        boolean mandatoryExists = type != null && peers != null;
        if (!mandatoryExists) {
            return null;
        }

        if (!type.equals("peers")) {
            return null;
        }

        for (String peer : peers) {
            String[] parts = peer.split(":");
            if (parts.length < 2 || parts[0].equals("") || parts[parts.length-1].equals("")) {
                continue;
            }

            // check port
            try {
                int port = Integer.valueOf(parts[parts.length - 1].trim());
                if (port < 1 || port > 65535)
                    continue;
            } catch (NumberFormatException exception) {
                continue;
            }

            String address = peer.substring(0, peer.lastIndexOf(":"));
            // check ip4
            String[] numbers = address.split("\\.");
            if (numbers.length == 4) {
                try {
                    int first = Integer.valueOf(numbers[0].trim());
                    int second = Integer.valueOf(numbers[1].trim());
                    int third = Integer.valueOf(numbers[2].trim());
                    int fourth = Integer.valueOf(numbers[3].trim());
                    if (first < 0 || first > 255
                            || second < 0 || second > 255
                            || third < 0 || third > 255
                            || fourth < 0 || fourth > 255) {
                        continue;
                    } else {
                        validPeers.add(peer);
                        continue;
                    }
                } catch (NumberFormatException exception) {
                    // then it's not an ip4-address
                }
            }

            // check ip6
            String ip6Regex = "(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))";
            Pattern pattern = Pattern.compile(ip6Regex);
            Matcher matcher = pattern.matcher(address);
            if(matcher.find()) {
                validPeers.add(peer);
                continue;
            }

            // check domain
            String[] names = address.split("\\.");
            if (names.length > 1) {
                validPeers.add(peer);
                continue;
            }

        }

        return validPeers;
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

//    public static void main(String[] args) throws Exception {
//        PeersMessage peers = new ObjectMapper().readValue("{\"type\" : \"peers\", \"peers\" : [" +
//                "\"****.com:18018\" ," +
//                "\"138.197.191.170:18018\", " +
//                "\"[fe80::f03c:91ff:fe2c:5a79]:18018\", " +
//                "\"128.130.122.101:18018\", " +
//                "\"20.23.212.159:18018\", " +
//                "\"84.112.193.9:18018\", " +
//                "\"143.244.205.206:18018\", " +
//                "\"kerma.duckdns.org:5253\"] }", PeersMessage.class);
//        peers.verifyPeersMessage();
//        int i = 0;
//    }
}
