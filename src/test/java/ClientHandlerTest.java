
import Util.Util;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import messages.GetPeersMessage;
import messages.HelloMessage;
import messages.PeersMessage;
import org.junit.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import static Util.Util.*;
import static org.junit.Assert.*;

public class ClientHandlerTest
{
    private static final String NEW_PEER_1 = "111.222.123.241:18018";
    private static final String NEW_PEER_2 = "9.8.52.123:18018";
    private static final String KERMA_SERVER_HOST = "localhost";
    private static final int KERMA_SERVER_PORT = 18018;

    private static ServerNode testServer;
    private static Thread testServerThread;
    private static ObjectMapper objectMapper;

    private static Socket clientSocket;
    private static PrintWriter out;
    private static BufferedReader in;

    @BeforeClass
    public static void setup() throws IOException
    {
        // Start a kerma server that we can use for testing
        int PORT = 18018;
        String IP_ADDRESS = "139.59.159.65";
        String name = "not node, hot implode";
        String versionOfNode = "0.9";
        List<String> listOfDiscoveredPeers = Util.readPeersOfPersistentFile("peers.txt");
        HashMap<String, Entities.Object> listOfObjects = new HashMap<>();

        Logger log = Logger.getLogger("logger");

        if (listOfDiscoveredPeers == null) {
            log.severe("ERROR - peers could not be read from file!");
            System.exit(-1);
        }

        objectMapper = new ObjectMapper();

        testServer = new ServerNode(PORT, IP_ADDRESS, name, versionOfNode, listOfDiscoveredPeers, listOfObjects, log);
        new Thread(() -> {
            testServer.launch();
        }).start();
    }

    @AfterClass
    public static void teardown()
    {
        testServer.shutdown();
    }

    @Before
    public void createServerSocketConnection() throws IOException
    {
        clientSocket = new Socket(KERMA_SERVER_HOST, KERMA_SERVER_PORT);

        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    @After
    public void closeServerSocketConnection() throws IOException, NullPointerException
    {
        in.close();
        out.close();
        clientSocket.close();
    }

    @Test
    public void when_clientConnectsAndSendsInvalidHello_then_serverClosesConnection()
            throws IOException
    {
        System.out.println("here");
        // Ignore server handshake
        in.readLine();
        in.readLine();

        // send invalid first message
        out.println("HELLO SERVER");
        out.flush();

        // Server responds with an error message
        JsonNode jso = objectMapper.readTree(in.readLine());

        assertTrue(jso.has("type"));
        assertEquals("\"error\"", jso.get("type").toString());

        // -1 on read indicates that the other party (the server in this case) has closed the connection
        // See: https://stackoverflow.com/a/10241044
        assertEquals(-1, in.read());
    }

    @Test(timeout=15000)
    public void when_clientSendsInvalidVersionInHandshake_then_serverReturnsError() throws IOException
    {
        in.readLine();
        in.readLine(); // Ignore server handshake

        out.println(objectMapper.writeValueAsString(new HelloMessage(hello, "0.9.1", "test-agent")));
        out.flush();

        JsonNode jso = objectMapper.readTree(in.readLine());

        assertTrue(jso.has("type"));
        assertEquals("\"error\"", jso.get("type").toString());
    }

    @Test(timeout=15000)
    public void when_clientOmitsVersionInHandshake_then_serverReturnsError() throws IOException
    {
        in.readLine(); // Ignore server handshake
        in.readLine(); // Ignore server handshake

        out.println( "{ \"type\" : \"hello\", \"agent\" : \"Kerma−Co \\n re Client 0.8\" }" );
        out.flush();

        JsonNode response = objectMapper.readTree(in.readLine());

        assertTrue(response.has("type"));
        assertEquals("\"error\"", response.get("type").toString());
    }

//    @Test(timeout=15000)
//    public void when_clientOmitsAgentInValidHandshake_then_serverAcceptsHandshake() throws IOException
//    {
//        in.readLine(); // Ignore server handshake
//        in.readLine(); // Ignore server handshake
//
//        out.println( "{ \"type\" : \"hello\", \"version\" : \"0.8.0\" }" );
//        out.flush();
//
//        JsonNode response = objectMapper.readTree(in.readLine());
//
//        assertTrue(response.has("type"));
//        assertNotEquals("\"error\"", response.get("type").toString()); // should be getpeers, but not relevant here
//    }

//    @Test(timeout=60000)
//    public void when_clientDoesNotSendAnyHandshake_then_serverClosesConnectionAfterTimeout()
//        throws IOException, InterruptedException
//    {
//
//    }
//
    @Test(timeout=15000)
    public void when_clientConnectsAndSendsHelloMessage_then_serverSendsValidHelloMessage() throws IOException
    {
        JsonNode jso = objectMapper.readTree(in.readLine());

        out.println(objectMapper.writeValueAsString(new HelloMessage(hello, "0.8.0", "test-agent")));
        out.flush();

        assertTrue(jso.has("type"));
        assertTrue(jso.has("agent"));
        assertTrue(jso.has("version"));
        assertEquals("\"hello\"", jso.get("type").toString());
        assertTrue(jso.get("version").toString().matches("\"0\\.8\\..\"+"));
    }

    @Test(timeout=15000)
    public void when_clientSendsInvalidHelloMessage_then_serverRespondsWithErrorMessage() throws IOException
    {
        String firstServerMessage = in.readLine(); // should be hello
        firstServerMessage = in.readLine(); // should be peers

        out.println("peers");
        out.flush();

        JsonNode errorJSON = objectMapper.readTree(in.readLine());

        assertTrue(errorJSON.has("type"));
        assertTrue(errorJSON.has("error"));
        assertEquals("\"error\"", errorJSON.get("type").toString());
    }

    @Test(timeout=15000)
    public void when_connectedClientSendsInvalidMessageAfterHello_then_serverRespondsWithErrorMessage()
            throws IOException
    {
        // This should contain the server's hello message. ignore this line
        in.readLine();
        // Send the client handshake
        out.println(objectMapper.writeValueAsString(new HelloMessage(hello, "0.8.0", "test-agent")));
        out.flush();

        // The first valid response from the server after the handshake must be a getpeers message. Ignore it, too
        in.readLine();

        // Send an invalid message right after the client handshake
        out.println("this is an invalid message");
        out.flush();

        // The next reply must be an error message
        JsonNode jso = objectMapper.readTree(in.readLine());

        assertTrue(jso.has("type"));
        assertEquals("\"error\"", jso.get("type").toString());
    }

    @Test(timeout=15000)
    public void when_connectedClientRequestsPeers_then_serverRespondsWithPeersMessage()
            throws IOException, InterruptedException
    {
        // This should contain the server's hello message. ignore this line
        in.readLine();

        // Send the client handshake
        out.println(objectMapper.writeValueAsString(new HelloMessage(hello, "0.8.0", "test-agent")));
        out.flush();

        // The first valid response from the server after the handshake must be a getpeers message. Ignore it, too
        in.readLine();

        // Request the peers
        out.println(objectMapper.writeValueAsString(new GetPeersMessage(getpeers)));
        out.flush();

        JsonNode jso = objectMapper.readTree(in.readLine());

        assertTrue(jso.has("type"));
        assertTrue(jso.has("peers"));
        assertEquals("\"peers\"", jso.get("type").toString());
    }

    @Test(timeout=15000)
    public void when_clientSentHello_then_serverImmediatelyRequestsPeers() throws IOException, InterruptedException
    {
        out.println(objectMapper.writeValueAsString(new HelloMessage(hello, "0.8.0", "test-agent")));

        // Ignore the server's hello message
        in.readLine();

        JsonNode jso = objectMapper.readTree(in.readLine());

        // Check, whether the second message (the first one immediately after the handshake) is a getpeers
        assertTrue(jso.has("type"));
        assertEquals("\"getpeers\"", jso.get("type").toString());
    }

    @Test
    public void when_clientSendsValidHandshakeInTwoSplitMessages_then_serverAcceptsHandshake()
            throws IOException, InterruptedException
    {
        in.readLine(); // Ignore server hello message
        in.readLine(); // Ignore server getpeers message

        String clientHello = objectMapper.writeValueAsString(new HelloMessage(hello, "0.8.0", "test-agent"));
        String clientHelloLeft = clientHello.substring(0, clientHello.length() / 2);
        String clientHelloRight = clientHello.substring(clientHelloLeft.length());

        out.print(clientHelloLeft); // do not send an end-of-line character here! assume an honest client...
        out.flush();

        Thread.sleep(1500);

        out.println(clientHelloRight);
        out.flush();

        // Request the peers
        out.println(objectMapper.writeValueAsString(new GetPeersMessage(getpeers)));
        out.flush();

        // The server should send a getpeers message right after accepting a client
        JsonNode jso = objectMapper.readTree(in.readLine());

        assertTrue(jso.has("type"));
        assertEquals("\"peers\"", jso.get("type").toString());
    }

    @Test(timeout=30000)
    public void when_clientOnlySendsFirstHalfOfValidHandshake_then_serverReturnsError()
            throws IOException, InterruptedException
    {
        in.readLine(); // Ignore server hello message
        in.readLine(); // Ignore server hello message

        String clientHello = objectMapper.writeValueAsString(new HelloMessage(hello, "0.8.0", "test-agent"));
        String clientHelloLeft = clientHello.substring(0, clientHello.length() / 2);

        out.println(clientHelloLeft); // send a newline character here -> invalid handshake (incomplete JSON)
        out.flush();

        JsonNode jso = objectMapper.readTree(in.readLine());
        assertTrue(jso.has("type"));
        assertEquals("\"error\"", jso.get("type").toString());
    }

    @Test(timeout=15000)
    public void when_connectedClientRequestsPeersInTwoSplitMessages_then_serverRespondsWithPeersMessage()
            throws IOException, InterruptedException
    {
        // This should contain the server's hello message. ignore this line
        in.readLine();

        // Send client handshake
        out.println( objectMapper.writeValueAsString(new HelloMessage(hello, "0.8.0", "test-agent")));
        out.flush();

        // Create two halves of getpeers request
        String clientRequest = objectMapper.writeValueAsString(new GetPeersMessage(getpeers));
        String clientRequestLeft = clientRequest.substring(0, clientRequest.length() / 2);
        String clientRequestRight = clientRequest.substring(clientRequestLeft.length());

        // send first half
        out.print(clientRequestLeft);
        out.flush();

        // Wait before sending the other half of the message
        Thread.sleep(1000);

        // Send second half
        out.println(clientRequestRight);
        out.flush();

        // Ignore the server's mandatory getpeers message
        String s = in.readLine();

        // Then read the server's response to the getpeers request
        JsonNode jso = objectMapper.readTree(in.readLine());

        assertTrue(jso.has("type"));
        assertTrue(jso.has("peers"));
        assertEquals("\"peers\"", jso.get("type").toString());
    }

    @Test(timeout=15000)
    public void when_connectedClientSendsMultipleMessagesAtOnce_then_serverSeparatesMessagesAndRepliesToEach()
        throws IOException
    {
        String allMessages = objectMapper.writeValueAsString(new HelloMessage(hello, "0.8.0", "test-agent")) + '\n' +
                objectMapper.writeValueAsString(new GetPeersMessage(getpeers)) + '\n' +
                objectMapper.writeValueAsString(new PeersMessage(peers, new ArrayList<>(Arrays.asList("22.33.44.55", "44.45.46.47"))));

        out.println(allMessages);
        out.flush();

        JsonNode hello = objectMapper.readTree(in.readLine());
        JsonNode getpeers = objectMapper.readTree(in.readLine());
        JsonNode peers = objectMapper.readTree(in.readLine());

        assertTrue(hello.has("type"));
        assertTrue(getpeers.has("type"));
        assertTrue(peers.has("type"));
        assertTrue(peers.has("peers"));

        assertEquals("\"hello\"", hello.get("type").toString());
        assertEquals("\"getpeers\"", getpeers.get("type").toString());
        assertEquals("\"peers\"", peers.get("type").toString());
    }

    @Test(timeout=15000)
    public void when_connectedClientDisconnects_then_clientShouldBeAbleToReconnect() throws IOException
    {
        // Ignore server's hello message
        in.readLine();

        in.close();
        out.close();
        clientSocket.close();

        clientSocket = new Socket(KERMA_SERVER_HOST, KERMA_SERVER_PORT);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        JsonNode hello = objectMapper.readTree(in.readLine());

        out.println(objectMapper.writeValueAsString(new HelloMessage(Util.hello, "0.8.0", "test-agent")));

        JsonNode getpeers = objectMapper.readTree(in.readLine());

        out.println( objectMapper.writeValueAsString(new GetPeersMessage(Util.getpeers)));
        out.println(objectMapper.writeValueAsString(new PeersMessage(Util.peers, new ArrayList<>(Arrays.asList("22.33.44.55", "44.45.46.47")))));

        JsonNode peers = objectMapper.readTree(in.readLine());

        assertTrue(hello.has("type"));
        assertTrue(getpeers.has("type"));
        assertTrue(peers.has("type"));
        assertTrue(peers.has("peers"));

        assertEquals("\"hello\"", hello.get("type").toString());
        assertEquals("\"getpeers\"", getpeers.get("type").toString());
        assertEquals("\"peers\"", peers.get("type").toString());
    }

    @Test(timeout=30000)
    public void when_clientConnectsMultipleTimes_then_serverHandlesAllRequestsCorrectly() throws IOException
    {
        Socket secondSocket = new Socket(KERMA_SERVER_HOST, KERMA_SERVER_PORT);
        PrintWriter secondOut;
        BufferedReader secondIn;

        secondOut = new PrintWriter(secondSocket.getOutputStream(), true);
        secondIn = new BufferedReader(new InputStreamReader(secondSocket.getInputStream()));

        JsonNode firstHello = objectMapper.readTree(in.readLine());
        JsonNode secondHello = objectMapper.readTree(secondIn.readLine());

        out.println(objectMapper.writeValueAsString(new HelloMessage(Util.hello, "0.8.0", "test-agent")));
        secondOut.println(objectMapper.writeValueAsString(new HelloMessage(Util.hello, "0.8.0", "test-agent")));

        JsonNode firstGetpeers = objectMapper.readTree(in.readLine());
        JsonNode secondGetpeers = objectMapper.readTree(secondIn.readLine());

        assertTrue(firstHello.has("type"));
        assertTrue(secondHello.has("type"));
        assertTrue(firstGetpeers.has("type"));
        assertTrue(secondGetpeers.has("type"));

        assertEquals("\"hello\"", firstHello.get("type").toString());
        assertEquals("\"hello\"", secondHello.get("type").toString());
        assertEquals("\"getpeers\"", firstGetpeers.get("type").toString());
        assertEquals("\"getpeers\"", secondGetpeers.get("type").toString());

        secondIn.close();
        secondOut.close();
        secondSocket.close();
    }

    @Test(timeout=300000)
    public void when_connectedClientSendsNewValidPeers_then_serverUpdatesPeerList() throws IOException, InterruptedException {
        // ignore server hello
        in.readLine();

        // send client handshake
        out.println(objectMapper.writeValueAsString(new HelloMessage(Util.hello, "0.8.0", "test-agent")));

        // read the server's getpeers request
        in.readLine();

        // send a getpeers request and read the server's peers response
        out.println(objectMapper.writeValueAsString(new GetPeersMessage(Util.getpeers)));
        JsonNode firstPeers = objectMapper.readTree(in.readLine());

        // Then, send the server a peers message that contains new (valid) entries
        out.println(objectMapper.writeValueAsString(new PeersMessage(Util.peers, new ArrayList<>(Arrays.asList(NEW_PEER_1, NEW_PEER_2)))));

        // Then disconnect and reconnect
        clientSocket.close();
        Thread.sleep(1500);
        clientSocket = new Socket(KERMA_SERVER_HOST, KERMA_SERVER_PORT);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // ignore server hello
        in.readLine();

        // send client handshake
        out.println(objectMapper.writeValueAsString(new HelloMessage(Util.hello, "0.8.0", "test-agent")));

        // read the server's getpeers request
        in.readLine();

        // Request the peers from the server again
        out.println(objectMapper.writeValueAsString(new GetPeersMessage(Util.getpeers)));
        JsonNode secondPeers = objectMapper.readTree(in.readLine());

        // Then compare the first and second peers responses:
        assertTrue(firstPeers.has("type"));
        assertTrue(secondPeers.has("type"));
        assertTrue(firstPeers.has("peers"));
        assertTrue(secondPeers.has("peers"));

        assertEquals("\"peers\"", firstPeers.get("type").toString());
        assertEquals("\"peers\"", secondPeers.get("type").toString());

        JsonNode firstArray = firstPeers.get("peers");
        JsonNode secondArray = secondPeers.get("peers");

        boolean firstArrayContainsNewFirstEntry = false;
        boolean firstArrayContainsNewSecondEntry = false;
        boolean secondArrayContainsNewFirstEntry = false;
        boolean secondArrayContainsNewSecondEntry = false;

        for (Object o : firstArray)
        {
            if(("\"" + NEW_PEER_1 + "\"").equalsIgnoreCase(o.toString()))
                firstArrayContainsNewFirstEntry = true;

            if(("\"" + NEW_PEER_2 + "\"").equalsIgnoreCase(o.toString()))
                firstArrayContainsNewSecondEntry = true;
        }

        for (Object o : secondArray)
        {
            if(("\"" + NEW_PEER_1 + "\"").equalsIgnoreCase(o.toString()))
                secondArrayContainsNewFirstEntry = true;

            if(("\"" + NEW_PEER_2 + "\"").equalsIgnoreCase(o.toString()))
                secondArrayContainsNewSecondEntry = true;
        }

        assertTrue(firstArray.size() < secondArray.size());
        assertFalse(firstArrayContainsNewFirstEntry);
        assertFalse(firstArrayContainsNewSecondEntry);
        assertTrue(secondArrayContainsNewFirstEntry);
        assertTrue(secondArrayContainsNewSecondEntry);
    }
}
