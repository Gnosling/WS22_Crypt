package Util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

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

    public static boolean isValidAddress(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
        } catch (UnknownHostException | IllegalArgumentException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
