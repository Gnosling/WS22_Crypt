package Util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class Util {

    public static boolean isJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readTree(json);
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
