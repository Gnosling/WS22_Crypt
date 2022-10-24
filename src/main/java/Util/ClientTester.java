package Util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

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
        request = "{ \"type\" : \"hello\", \"version\" : \"0.8.0\", \"agent\" : \"Kerma−Core Client 0.8\" }";
        writer.println(request);
        writer.flush();
        // second send getpeers
        request = "{ \"type\" : \"getpeers\" }";
        writer.println(request);
        writer.flush();


        // expect first an initial hello
        response = reader.readLine();
        System.out.println("First hello: " + response);
        // expect then the getpeers-request
        response = reader.readLine();
        System.out.println("Get peers: " + response);


        // expect then the response-hello
        response = reader.readLine();
        System.out.println("Second hello: " + response);
        // expect then a list of peers
        response = reader.readLine();
        System.out.println("Peers: " + response);


        // send peers
        request = "{\"type\" : \"peers\", \"peers\" : [\"****.com:18018\" ,\"138.197.191.170:18018\", \"[fe80::f03c:91ff:fe2c:5a79]:18018\"] }";
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
        writer.println(request);
        writer.flush();
        TimeUnit.MILLISECONDS.sleep(100);
        request = "tpeers\" }";
        writer.println(request);
        writer.flush();
        // and expect peers
        response = reader.readLine();
        System.out.println("Peers: " + response);
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

        // send getpeers before hello
        request = "{ \"type\" : \"getpeers\" }";
        writer.println(request);
        writer.flush();
        // and expect error
        response = reader.readLine();
        System.out.println("Expected error: " + response);

        // send invalid message
        request = "adasvevsvew";
        // request = " \"type\":\"diufygeuybhv\" ";
        // request = "{ \"type\":\"diufygeuybhv\" }";
        // request = "{ \"type\":\"hello\" }";
        // request = "{ \"type\":\"hello\", \"version\":\"5.8.2\" }";
        writer.println(request);
        writer.flush();
        // and expect error
        response = reader.readLine();
        System.out.println("Expected error: " + response);
    }

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 18018;
        testValidRequests(host, port);
        testInvalidRequests(host, port);
    }
}
