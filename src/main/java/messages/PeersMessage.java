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
            int port = 0;
            try {
                port = Integer.valueOf(parts[parts.length - 1].trim());
                if (port < 1 || port > 65535)
                    continue;
            } catch (NumberFormatException exception) {
                continue;
            }

            String address = peer.substring(0, peer.lastIndexOf(":")).trim();

            List<String> forbiddenAddresses = new ArrayList<>();
            forbiddenAddresses.add("139.59.159.65");
            forbiddenAddresses.add("142.93.167.83");
            forbiddenAddresses.add("164.92.143.182");
            forbiddenAddresses.add("20.126.29.4");
            forbiddenAddresses.add("20.23.212.159");
            forbiddenAddresses.add("213.47.118.194");
            forbiddenAddresses.add("3.120.126.31");
            forbiddenAddresses.add("3.126.74.45");
            forbiddenAddresses.add("3.83.80.138");
            forbiddenAddresses.add("45.156.242.189");
            forbiddenAddresses.add("46.101.139.223");
            forbiddenAddresses.add("62.178.13.17");
            forbiddenAddresses.add("62.178.51.126");
            forbiddenAddresses.add("62.99.145.254");
            forbiddenAddresses.add("77.119.167.6");
            forbiddenAddresses.add("78.104.120.205");
            forbiddenAddresses.add("84.113.167.151");
            forbiddenAddresses.add("84.113.55.218");
            forbiddenAddresses.add("84.115.210.133");
            forbiddenAddresses.add("84.115.217.219");
            forbiddenAddresses.add("84.115.221.58");
            forbiddenAddresses.add("84.115.223.186");
            forbiddenAddresses.add("84.115.238.55");
            forbiddenAddresses.add("84.115.223.186");
            forbiddenAddresses.add("84.115.238.55");
            forbiddenAddresses.add("85.127.43.140");
            forbiddenAddresses.add("85.127.44.22");
            forbiddenAddresses.add("88.200.23.239");
            forbiddenAddresses.add("91.141.46.117");


            List<String> duplicatedAddressesWithUnwantedPorts = new ArrayList<>();
            duplicatedAddressesWithUnwantedPorts.add("104.248.100.76");
            duplicatedAddressesWithUnwantedPorts.add("134.122.88.104");
            duplicatedAddressesWithUnwantedPorts.add("128.130.239.253");
            duplicatedAddressesWithUnwantedPorts.add("128.130.241.65");
            duplicatedAddressesWithUnwantedPorts.add("128.130.245.236");
            duplicatedAddressesWithUnwantedPorts.add("128.130.246.129");
            duplicatedAddressesWithUnwantedPorts.add("128.131.236.185");
            duplicatedAddressesWithUnwantedPorts.add("138.197.177.229");
            duplicatedAddressesWithUnwantedPorts.add("138.246.3.59");
            duplicatedAddressesWithUnwantedPorts.add("139.59.136.230");
            duplicatedAddressesWithUnwantedPorts.add("152.32.171.91");
            duplicatedAddressesWithUnwantedPorts.add("157.230.31.236");
            duplicatedAddressesWithUnwantedPorts.add("157.230.99.2");
            duplicatedAddressesWithUnwantedPorts.add("158.255.211.213");
            duplicatedAddressesWithUnwantedPorts.add("159.65.119.144");
            duplicatedAddressesWithUnwantedPorts.add("159.65.119.249");
            duplicatedAddressesWithUnwantedPorts.add("159.89.9.134");
            duplicatedAddressesWithUnwantedPorts.add("161.35.21.146");
            duplicatedAddressesWithUnwantedPorts.add("161.35.223.236");
            duplicatedAddressesWithUnwantedPorts.add("165.227.152.155");
            duplicatedAddressesWithUnwantedPorts.add("167.235.73.152");
            duplicatedAddressesWithUnwantedPorts.add("167.71.36.88");
            duplicatedAddressesWithUnwantedPorts.add("167.99.240.225");
            duplicatedAddressesWithUnwantedPorts.add("167.172.103.227");
            duplicatedAddressesWithUnwantedPorts.add("172.21.0.1");
            duplicatedAddressesWithUnwantedPorts.add("173.212.194.142");
            duplicatedAddressesWithUnwantedPorts.add("178.191.175.17");
            duplicatedAddressesWithUnwantedPorts.add("178.191.208.59");
            duplicatedAddressesWithUnwantedPorts.add("179.60.149.55");
            duplicatedAddressesWithUnwantedPorts.add("185.222.241.212");
            duplicatedAddressesWithUnwantedPorts.add("185.230.160.111");
            duplicatedAddressesWithUnwantedPorts.add("193.122.4.42");
            duplicatedAddressesWithUnwantedPorts.add("193.81.255.119");
            duplicatedAddressesWithUnwantedPorts.add("207.154.203.135");
            duplicatedAddressesWithUnwantedPorts.add("212.186.0.111");
            duplicatedAddressesWithUnwantedPorts.add("213.143.126.35");
            duplicatedAddressesWithUnwantedPorts.add("213.208.157.39");
            duplicatedAddressesWithUnwantedPorts.add("213.208.157.89");
            duplicatedAddressesWithUnwantedPorts.add("213.225.33.148");
            duplicatedAddressesWithUnwantedPorts.add("213.225.36.81");
            duplicatedAddressesWithUnwantedPorts.add("213.225.38.187");
            duplicatedAddressesWithUnwantedPorts.add("31.12.0.218");
            duplicatedAddressesWithUnwantedPorts.add("34.75.199.41");
            duplicatedAddressesWithUnwantedPorts.add("37.252.191.95");
            duplicatedAddressesWithUnwantedPorts.add("45.40.56.112");
            duplicatedAddressesWithUnwantedPorts.add("45.87.212.182");
            duplicatedAddressesWithUnwantedPorts.add("45.156.242.133");
            duplicatedAddressesWithUnwantedPorts.add("46.101.172.240");
            duplicatedAddressesWithUnwantedPorts.add("46.125.250.33");
            duplicatedAddressesWithUnwantedPorts.add("54.210.100.81");
            duplicatedAddressesWithUnwantedPorts.add("54.38.159.64");
            duplicatedAddressesWithUnwantedPorts.add("64.227.114.191");
            duplicatedAddressesWithUnwantedPorts.add("68.183.208.28");
            duplicatedAddressesWithUnwantedPorts.add("68.183.223.214");
            duplicatedAddressesWithUnwantedPorts.add("77.119.104.129");
            duplicatedAddressesWithUnwantedPorts.add("77.119.168.205");
            duplicatedAddressesWithUnwantedPorts.add("80.108.106.200");
            duplicatedAddressesWithUnwantedPorts.add("80.108.95.155");
            duplicatedAddressesWithUnwantedPorts.add("80.109.197.86");
            duplicatedAddressesWithUnwantedPorts.add("80.211.208.15");
            duplicatedAddressesWithUnwantedPorts.add("81.217.107.219");
            duplicatedAddressesWithUnwantedPorts.add("81.217.189.82");
            duplicatedAddressesWithUnwantedPorts.add("84.112.27.193");
            duplicatedAddressesWithUnwantedPorts.add("84.115.210.138");
            duplicatedAddressesWithUnwantedPorts.add("84.115.229.99");
            duplicatedAddressesWithUnwantedPorts.add("84.115.238.131");
            duplicatedAddressesWithUnwantedPorts.add("84.115.239.208");
            duplicatedAddressesWithUnwantedPorts.add("85.127.92.186");
            duplicatedAddressesWithUnwantedPorts.add("86.48.7.161");
            duplicatedAddressesWithUnwantedPorts.add("86.48.7.189");
            duplicatedAddressesWithUnwantedPorts.add("86.56.242.179");
            duplicatedAddressesWithUnwantedPorts.add("89.142.138.94");
            duplicatedAddressesWithUnwantedPorts.add("89.144.201.60");
            duplicatedAddressesWithUnwantedPorts.add("89.144.203.14");
            duplicatedAddressesWithUnwantedPorts.add("89.144.221.139");
            duplicatedAddressesWithUnwantedPorts.add("91.113.41.50");
            duplicatedAddressesWithUnwantedPorts.add("91.118.114.136");
            duplicatedAddressesWithUnwantedPorts.add("92.248.60.17");
            duplicatedAddressesWithUnwantedPorts.add("95.102.49.196");


            if (address.equals("")
                    || address.equals("localhost")
                    || address.equals("127.0.0.0")
                    || address.equals("1.1.1.1")
                    || address.equals("8.8.8.8")) {
                continue;
            }

            if (forbiddenAddresses.contains(address)) {
                continue;
            }

            if (duplicatedAddressesWithUnwantedPorts.contains(address) && port != 18018) {
                continue;
            }

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
