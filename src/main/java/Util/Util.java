package Util;

import Entities.Object;
import Entities.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.Security;
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

    public static String computeHash(String input) {
        String sha256hex = Hashing.sha256()
                .hashString(input, StandardCharsets.UTF_8)
                .toString();
        return sha256hex;
    }

    public static boolean verifySignature(String pubKey, String message, String signature) throws CryptoException {
        Ed25519PublicKeyParameters publicKey = new Ed25519PublicKeyParameters(BaseEncoding.base16().decode(pubKey.toUpperCase()));

        // Verify
        byte[] messageAsBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] signatureAsBytes = BaseEncoding.base16().decode(signature.toUpperCase());

        Signer verifier = new Ed25519Signer();
        verifier.init(false, publicKey);
        verifier.update(messageAsBytes, 0, messageAsBytes.length);
        return verifier.verifySignature(signatureAsBytes);
    }

//    public static void main(String[] args) throws Exception {
//        boolean o = verifySignature("62b7c521cd9211579cf70fd4099315643767b96711febaa5c76dc3daf27c281c", "{\"inputs\":[{\"outpoint\":{\"index\":0,\"txid\":\"48c2ae2fbb4dead4bcc5801f6eaa9a350123a43750d22d05c53802b69c7cd9fb\"},\"sig\":null}],\"outputs\":[{\"pubkey\":\"228ee807767047682e9a556ad1ed78dff8d7edf4bc2a5f4fa02e4634cfcad7e0\",\"value\":49000000000000}],\"type\":\"transaction\"}", "d51e82d5c121c5db21c83404aaa3f591f2099bccf731208c4b0b676308be1f994882f9d991c0ebfd8fdecc90a4aec6165fc3440ade9c83b043cba95b2bba1d0a");
//        System.out.println(o);
//        int i = 0;
//    }

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
