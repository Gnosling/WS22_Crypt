package Util;

import com.fasterxml.jackson.databind.ObjectMapper;
import messages.GetPeersMessage;
import messages.PeersMessage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static Util.Util.isParsableInJson;

public class ClientTester {

    public static void testValidRequests(String host, int port) throws Exception {
        Socket clientSocket = new Socket(host, port);
        clientSocket.setSoTimeout(1000*90); // terminate after 90s

        // prepare the input reader for the socket
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        // prepare the writer for responding to clients requests
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());

        String request = "";
        String response = "";

        // first send hello
        request = "{ \"type\" : \"hello\", \"version\" : \"0.8.3\", \"agent\" : \"Kerma−Core Client 0.8\" }";
        writer.println(request);
        // second send getpeers
        request = "{ \"type\" : \"getpeers\" }";
//        request = "{ \"type\":\"getpeers\", \"type\":\"getpeers\" }";
        writer.println(request);
        writer.flush();

        System.out.println("ready to read!");


        // expect first an initial hello
        response = reader.readLine();
        System.out.println("First hello: " + response);
        // expect then the getpeers-request
        response = reader.readLine();
        System.out.println("Get peers: " + response);


        // expect then a list of peers
        response = reader.readLine();
        System.out.println("Peers: " + response);

        // send hello again
//        request = "{ \"type\":\"hello\", \"version\":\"0.8.0\" }";

        // send peers
        request = "{\"type\" : \"peers\", \"peers\" : [\"localhost:18018\" ,\"127.0.0.0:18018\", \"1.1.1.1:18018\", \"8.8.8.8:18018\"] }";
//        request = "{ \"type\":\"peers\", \"peers\":[] }";
        writer.println(request);
        writer.flush();
        // and ask extended peers
        request = "{ \"type\" : \"getpeers\" }";
        writer.println(request);
        writer.flush();
        // expect then a list of extended peers
        response = reader.readLine();
        System.out.println("Extended peers: " + response);


        // send splitted message
        request = "{ \"type\" : \"ge";
        writer.print(request);
        writer.flush();
        TimeUnit.MILLISECONDS.sleep(100);
        request = "tpeers\" }";
        writer.println(request);
        writer.flush();
        // and expect peers
        response = reader.readLine();
        System.out.println("Peers (split): " + response);

        clientSocket.close();
    }

    public static void testInvalidRequests(String host, int port) throws Exception {
        Socket clientSocket = new Socket(host, port);
        clientSocket.setSoTimeout(1000*90); // terminate after 90s

        // prepare the input reader for the socket
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        // prepare the writer for responding to clients requests
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());

        String request = "";
        String response = "";

        // expect first an initial hello
        response = reader.readLine();
        System.out.println("First hello: " + response);

        // expect then the getpeers-request
        response = reader.readLine();
        System.out.println("Get peers: " + response);

        // first send hello
        request = "{ \"type\" : \"hello\", \"version\" : \"0.8.3\", \"agent\" : \"Kerma−Core Client 0.8\" }";
        writer.println(request);

//        // send getpeers before hello
//        request = "{ \"type\" : \"getpeers\" }";
//        writer.println(request);
//        writer.flush();
//        // and expect error
//        response = reader.readLine();
//        System.out.println("Expected error: " + response);

        // send invalid message
//        request = "adasvevsvew";
         request = " \"type\":\"diufygeuybhv\" ";
//         request = "{ \"type\":\"diufygeuybhv\" }";
//         request = "{ \"type\":\"hello\" }";
//         request = "{ \"type\":\"hello\", \"version\":\"5.8.2\" }";
//        request = "{ \"type\":\"hello\", \"version\":\"0.8.2\", \"invalid\": \"ddvdvvd\"}";
//        request = "{ \"type\":\"getPeers\" }";
//        request = "{ \"type\":\"getpeers\", \"version\":\"0.8.0\" }";
//        request = "{ \"type\":\"error\"}";
//        request = "{ \"type\":\"error\", \"error\":\"mimomi\" }";
//        request = "{ \"type\":\"peers\", \"peers\":\"12.23.45.67\" }";
//        request = "{ \"type\":\"peers\", \"peers\":[\"5.8.2\"] }";
//        request = "{ \"type\":\"peers\", \"peers\":null }";
        writer.println(request);
        writer.flush();
        // and expect error
        response = reader.readLine();
        System.out.println("Expected error: " + response);
    }

    public static void main(String[] args) throws Exception {

        int i = 0;
        String host = "localhost";
        int port = 18018;
//        testValidRequests(host, port);
        testInvalidRequests(host, port);
    }
}
