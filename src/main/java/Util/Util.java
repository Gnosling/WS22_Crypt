package Util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Util {

    public static final String error = "error";
    public static final String hello = "hello";
    public static final String getpeers = "getpeers";
    public static final String peers = "peers";


    public static boolean isJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readTree(json);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean isConnectableAddress(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isParsableInJson(ObjectMapper objectMapper, String object, Class type) {
        try {
            objectMapper.readValue(object, type);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    public static List<String> readPeersOfPersistentFile(String fileName) {
        BufferedReader reader = null;
        List<String> peers = new ArrayList<>();
        try {
            reader = new BufferedReader(new FileReader(fileName));
            String peer;
            while ((peer = reader.readLine()) != null) {
                if(!peer.equals("")) {
                    peers.add(peer);
                }
            }
            reader.close();
            return peers;
        } catch (IOException exception) {
            return null;
        }
    }

    public static boolean storePeersOnPersistentFile(List<String> peers, String fileName) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(fileName));
            for (String peer : peers) {
                writer.write(peer + "\n");
            }
            writer.close();
            return true;

        } catch (IOException exception) {
            return false;
        }
    }
}
