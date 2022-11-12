package Util;

import Entities.Object;
import Entities.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.hash.Hashing;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Util {

    public static final String error = "error";
    public static final String hello = "hello";
    public static final String getpeers = "getpeers";
    public static final String peers = "peers";
    public static final String getobject = "getobject";
    public static final String ihaveobject = "ihaveobject";
    public static final String object = "object";


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

    public static HashMap<String, Object> readObjectsOfPersistentFile(String fileName) {
        BufferedReader reader = null;
        ObjectMapper objectMapper = new ObjectMapper();
        HashMap<String, Object> objects = new HashMap<>();
        try {
            reader = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = reader.readLine()) != null) {
                if(!line.equals("")) {
                    String[] parts = line.split("~");
                    if(parts.length >= 2){
                        String key = parts[0].trim();
                        String body = line.substring(key.length()+1).trim();
                        if(isJson(body)) {
                            Object object = objectMapper.readValue(body, Object.class);
                            objects.put(key, object);
                        }
                    }
                }
            }
            reader.close();
            return objects;
        } catch (IOException exception) {
            return null;
        }
    }

    public static boolean appendObjectsOnPersistentFile(HashMap<String, Object> objects, String fileName) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(fileName, true));
            for (Map.Entry<String, Object> object :
                    objects.entrySet()) {

                // put key and value separated
                writer.write(object.getKey() + "~" + object.getValue().toJson());
                // new line
                writer.newLine();
            }
            writer.close();
            return true;

        } catch (IOException exception) {
            return false;
        }
    }
}
