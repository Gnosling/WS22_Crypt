package Util;

import Entities.Block;
import Entities.Object;
import Entities.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import java.io.*;
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

    public static final String hashIdOfGenesisBlock = "00000000a420b7cefa2b7730243316921ed59ffe836e111ca3801f82a4f5360e";


    public static boolean isJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readTree(json);
        } catch (IOException e) {
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

    public static HashMap<String, Object> readObjectsOfPersistentFile(String fileName, String fileNameOfStoredUTXOs) {
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
                            if (object instanceof Block) {
                                Block block = (Block) object;
                                HashMap<String, List<ContainerOfUTXO>> utxo = readUTXOOfPersistentFileForHashOfBlock(fileNameOfStoredUTXOs, key);
                                block.setUTXO(utxo);
                                objects.put(key, block);
                            } else {
                                objects.put(key, object);
                            }
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

    public static HashMap<String, List<ContainerOfUTXO>> readUTXOOfPersistentFileForHashOfBlock(String fileName, String blockHash) {
        BufferedReader reader = null;
        HashMap<String, List<ContainerOfUTXO>> utxo = new HashMap<>();
        try {
            reader = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = reader.readLine()) != null) {
                if(!line.equals("")) {
                    String[] parts = line.split("~");
                    String key = parts[0].trim();
                    if (!key.equals(blockHash)) {
                        continue;
                    }

                    if (parts.length == 1) {
                        reader.close();
                        return utxo;
                    } else if (parts.length >= 2){
                        if (parts[1].trim().equals("")) {
                            reader.close();
                            return utxo; // there are no entries
                        }
                        String body = line.substring(key.length()+1).trim(); //txHash~(index, flag);(i,f);(i,f)#txHash~(...);(...)
                        String[] txsWithUnspentOutput = body.split("#");

                        for (String entry : txsWithUnspentOutput) {
                            if (entry.equals("")) {
                                continue;
                            }

                            String txHash = entry.substring(0, entry.indexOf("~"));
                            String innerBody = entry.substring(txHash.length()+1).trim();
                            String[] pairs = innerBody.split(";");
                            List<ContainerOfUTXO> containerList = new ArrayList<>();

                            for (String pair : pairs) {
                                pair = pair.trim();
                                if (pair.equals("")) {
                                    continue;
                                }
                                pair = pair.substring(1, pair.length() - 1); // remove bracket
                                int index = Integer.parseInt(pair.substring(0, pair.indexOf(",")));
                                boolean flag = Boolean.parseBoolean(pair.substring(pair.indexOf(",") + 1));
                                containerList.add(new ContainerOfUTXO(index, flag));
                            }
                            utxo.put(txHash, containerList);
                        }
                        reader.close();
                        return utxo;
                    }
                }
            }
            reader.close();
            return utxo;
        } catch (IOException exception) {
            return null;
        }
    }

    public static boolean appendUTXOOnPersistentFileForHashOfBlock(String fileName, String blockHash, HashMap<String, List<ContainerOfUTXO>> utxo) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(fileName, true));
            StringBuilder body = new StringBuilder();
            for (Map.Entry<String, List<ContainerOfUTXO>> elem : utxo.entrySet()) {
                StringBuilder containerList = new StringBuilder();
                for (ContainerOfUTXO con : elem.getValue()) {
                    containerList.append("(" + con.getIndex() + "," + con.getIsUnspent() + ");");
                }
                body.append(elem.getKey() + "~" + containerList + "#");
            }
            writer.write(blockHash + "~" + body);
            writer.newLine();
            writer.close();
            return true;

        } catch (IOException exception) {
            return false;
        }
    }

    public static void main(String[] args) {
        byte[] messageAsBytes = "a26d92800cf58e88a5ecf37156c031a4147c2128beeaf1cca2785c93242a4c8b".getBytes(StandardCharsets.UTF_8);
        Object o = new Transaction();
        List<Long> s = new ArrayList<>();
        s.add(Long.valueOf(0)); s.add(12L);
        int i = 0;

    }
}
