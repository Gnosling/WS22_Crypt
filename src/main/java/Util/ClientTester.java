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

import static Util.Util.isJson;
import static Util.Util.isParsableInJson;

public class ClientTester {

    public static void testValidRequests(String host, int port) throws Exception {
        System.out.println("started");
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

        // HANDSHAKE done

        // send splitted message
//        request = "{ \"type\" : \"ge";
//        writer.print(request);
//        writer.flush();
//        TimeUnit.MILLISECONDS.sleep(100);
//        request = "tpeers\" }";
//        writer.println(request);
//        writer.flush();
//        // and expect peers
//        response = reader.readLine();
//        System.out.println("Peers (split): " + response);


//        request = "{\n" +
//                "\"type\" : \"object\" ,\n" +
//                "\"object\" : {\n" +
//                "\"type\" : \"block\" ,\n" +
//                "\"txids\" : [\"740bcfb434c89abe57bb2bc80290cd5495e87ebf8cd0dadb076bc50453590104\"],\n" +
//                "\"nonce\" : \"a26d92800cf58e88a5ecf37156c031a4147c2128beeaf1cca2785c93242a4c8b\" ,\n" +
//                "\"previd\" : \"0024839ec9632d382486ba7aac7e0bda3b4bda1d4bd79be9ae78e7e1e813ddd8\" ,\n" +
//                "\"created\" :1622825642,\n" +
//                "\"T\" : \"003a000000000000000000000000000000000000000000000000000000000000\",\n" +
//                "\"miner\":\"*****\"," +
//                "\"note\":\"A sample block\"" +
//                "}\n" +
//                "}";

//        request = "{\"object\":{\"inputs\":[{\"outpoint\":{\"index\":0,\n" +
//                "\"txid\":\"1bb37b637d07100cd26fc063dfd4c39a7931cc88dae3417871219715a5e374af\"},\n" +
//                "\"sig\":\"1d0d7d774042607c69a87ac5f1cdf92bf474c25fafcc089fe667602bfefb0494" +
//                "726c519e92266957429ced875256e6915eb8cea2ea66366e739415efc47a6805\"}],\n" +
//                "\"outputs\":[{\n" +
//                "\"pubkey\":\"8dbcd2401c89c04d6e53c81c90aa0b551cc8fc47c0469217c8f5cfbae1e911f9\",\n" +
//                "\"value\":10}],\"type\":\"transaction\"},\"type\":\"object\"}";
//
//        request = "{\"object\":{\"height\":0,\"outputs\":[{\n" +
//                "\"pubkey\":\"8dbcd2401c89c04d6e53c81c90aa0b551cc8fc47c0469217c8f5cfbae1e911f9\",\n" +
//                "\"value\":50000000000}],\"type\":\"transaction\"},\"type\":\"object\"}\n";

//        request = "{\"height\":1,\"outputs\":[{\"pubkey\":\"62b7c521cd9211579cf70fd4099315643767b96711febaa5c76dc3daf27c281c\",\"value\":50000000000000}],\"type\":\"transaction\"}";



//        // TEST coinbase transaction for new object
//        // Send Ihaveobject
//        request = "{\"type\":\"ihaveobject\",\"objectid\":\"48c2ae2fbb4dead4bcc5801f6eaa9a350123a43750d22d05c53802b69c7cd9fb\"}";
//        writer.println(request);
//        writer.flush();
//        response = reader.readLine(); // expect getobject
//        System.out.println("Getobject: " + response);
        // Send object
//        request = "{ \"object\": {\"height\":1,\"outputs\":[{\"pubkey\":\"62b7c521cd9211579cf70fd4099315643767b96711febaa5c76dc3daf27c281c\",\"value\":34567}],\"type\":\"transaction\"}, " +
//                "\"type\":\"object\"}";
//        request = "{\"object\":{\"height\":0,\"outputs\":[{" +
//                "\"pubkey\":\"8dbcd2401c89c04d6e53c81c90aa0b551cc8fc47c0469217c8f5cfbae1e911f9\"," +
//                "\"value\":50000000000}],\"type\":\"transaction\"},\"type\":\"object\"}";
//        request = "{\"object\":{\"inputs\":[{\"outpoint\":{\"index\":0," +
//                "\"txid\":\"1bb37b637d07100cd26fc063dfd4c39a7931cc88dae3417871219715a5e374af\"}," +
//                "\"sig\":\"1d0d7d774042607c69a87ac5f1cdf92bf474c25fafcc089fe667602bfefb0494" +
//                "726c519e92266957429ced875256e6915eb8cea2ea66366e739415efc47a6805\"}]," +
//                "\"outputs\":[{" +
//                "\"pubkey\":\"8dbcd2401c89c04d6e53c81c90aa0b551cc8fc47c0469217c8f5cfbae1e911f9\"," +
//                "\"value\":50000}],\"type\":\"transaction\"},\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        response = reader.readLine(); // expect original object
//        System.out.println("Response: " + response);
        // Send getobject
//        request = "{\"type\":\"getobject\",\"objectid\":\"48c2ae2fbb4dead4bcc5801f6eaa9a350123a43750d22d05c53802b69c7cd9fb\"}";
//        writer.println(request);
//        writer.flush();
//        response = reader.readLine(); // expect original object
//        System.out.println("Object: " + response);
//
//        // TEST coinbase transaction for existing object
//        // Send Ihaveobject
//        request = "{\"type\":\"ihaveobject\",\"objectid\":\"48c2ae2fbb4dead4bcc5801f6eaa9a350123a43750d22d05c53802b69c7cd9fb\"}";
//        writer.println(request);
//        writer.flush();
//        // expect no answer
//        // Send object
//        request = "{ \"object\": {\"height\":1,\"outputs\":[{\"pubkey\":\"62b7c521cd9211579cf70fd4099315643767b96711febaa5c76dc3daf27c281c\",\"value\":50000000000000}],\"type\":\"transaction\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        // expect no answer
//        // Send getobject
//        request = "{\"type\":\"getobject\",\"objectid\":\"48c2ae2fbb4dead4bcc5801f6eaa9a350123a43750d22d05c53802b69c7cd9fb\"}";
//        writer.println(request);
//        writer.flush();
//        response = reader.readLine(); // expect original object
//        System.out.println("Object: " + response);
//
//        // TEST transaction for new object
//        // Send Ihaveobject
//        request = "{\"type\":\"ihaveobject\",\"objectid\":\"d33ac384ea704025a6cac53f669c8e924eff7205b0cd0d6a231f0881b6265a8e\"}";
//        writer.println(request);
//        writer.flush();
//        response = reader.readLine(); // expect getobject
//        System.out.println("Getobject: " + response);
//        // Send object
//        request = "{\"object\": {\"inputs\":[{\"outpoint\":{\"index\":0,\"txid\":\"48c2ae2fbb4dead4bcc5801f6eaa9a350123a43750d22d05c53802b69c7cd9fb\"},\"sig\":\"d51e82d5c121c5db21c83404aaa3f591f2099bccf731208c4b0b676308be1f994882f9d991c0ebfd8fdecc90a4aec6165fc3440ade9c83b043cba95b2bba1d0a\"}],\"outputs\":[{\"pubkey\":\"228ee807767047682e9a556ad1ed78dff8d7edf4bc2a5f4fa02e4634cfcad7e0\",\"value\":49000000000000}],\"type\":\"transaction\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        // Send getobject
//        request = "{\"type\":\"getobject\",\"objectid\":\"d33ac384ea704025a6cac53f669c8e924eff7205b0cd0d6a231f0881b6265a8e\"}";
//        writer.println(request);
//        writer.flush();
//        response = reader.readLine(); // expect original object
//        System.out.println("Object: " + response);
//
//        // TEST transaction for existing object
//        // Send Ihaveobject
//        request = "{\"type\":\"ihaveobject\",\"objectid\":\"d33ac384ea704025a6cac53f669c8e924eff7205b0cd0d6a231f0881b6265a8e\"}";
//        writer.println(request);
//        writer.flush();
//        // expect no answer
//        // Send object
//        request = "{\"object\": {\"inputs\":[{\"outpoint\":{\"index\":0,\"txid\":\"48c2ae2fbb4dead4bcc5801f6eaa9a350123a43750d22d05c53802b69c7cd9fb\"},\"sig\":\"d51e82d5c121c5db21c83404aaa3f591f2099bccf731208c4b0b676308be1f994882f9d991c0ebfd8fdecc90a4aec6165fc3440ade9c83b043cba95b2bba1d0a\"}],\"outputs\":[{\"pubkey\":\"228ee807767047682e9a556ad1ed78dff8d7edf4bc2a5f4fa02e4634cfcad7e0\",\"value\":49000000000000}],\"type\":\"transaction\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        // expect no answer
//        // Send getobject
//        request = "{\"type\":\"getobject\",\"objectid\":\"d33ac384ea704025a6cac53f669c8e924eff7205b0cd0d6a231f0881b6265a8e\"}";
//        writer.println(request);
//        writer.flush();
//        response = reader.readLine(); // expect original object
//        System.out.println("Object: " + response);
//
//
//        // TEST block for new object
//        // Send Ihaveobject
//        request = "{\"type\":\"ihaveobject\",\"objectid\":\"00000000a420b7cefa2b7730243316921ed59ffe836e111ca3801f82a4f5360e\"}";
//        writer.println(request);
//        writer.flush();
//        response = reader.readLine(); // expect getobject
//        System.out.println("Getobject: " + response);
//        // Send object
//        request = "{\"object\": {\"T\":\"00000002af000000000000000000000000000000000000000000000000000000\",\"created\":1624219079,\"miner\":\"dionyziz\",\"nonce\":\"0000000000000000000000000000000000000000000000000000002634878840\",\"note\":\"The Economist 2021-06-20: Crypto-miners are probably to blame for the graphics-chip shortage\",\"previd\":null,\"txids\":[],\"type\":\"block\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        // Send getobject
//        request = "{\"type\":\"getobject\",\"objectid\":\"00000000a420b7cefa2b7730243316921ed59ffe836e111ca3801f82a4f5360e\"}";
//        writer.println(request);
//        writer.flush();
//        response = reader.readLine(); // expect original object
//        System.out.println("Object: " + response);
//
//        // TEST block for existing object
//        // Send Ihaveobject
//        request = "{\"type\":\"ihaveobject\",\"objectid\":\"00000000a420b7cefa2b7730243316921ed59ffe836e111ca3801f82a4f5360e\"}";
//        writer.println(request);
//        writer.flush();
//        // expect no answer
//        // Send object
//        request = "{\"object\": {\"T\":\"00000002af000000000000000000000000000000000000000000000000000000\",\"created\":1624219079,\"miner\":\"dionyziz\",\"nonce\":\"0000000000000000000000000000000000000000000000000000002634878840\",\"note\":\"The Economist 2021-06-20: Crypto-miners are probably to blame for the graphics-chip shortage\",\"previd\":null,\"txids\":[],\"type\":\"block\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        // expect no answer
//        // Send getobject
//        request = "{\"type\":\"getobject\",\"objectid\":\"00000000a420b7cefa2b7730243316921ed59ffe836e111ca3801f82a4f5360e\"}";
//        writer.println(request);
//        writer.flush();
//        response = reader.readLine(); // expect original object
//        System.out.println("Object: " + response);




        // ____________________________________________________________________________________________________________________________________________________
        //                          --- --- ------ --- ---
        //                          --- --- TASK 3 --- ---
        //                          --- --- ------ --- ---
        // ____________________________________________________________________________________________________________________________________________________
        // Send genesis
//        System.out.println("send genesis");
//        request = "{\"object\": {\"T\":\"00000002af000000000000000000000000000000000000000000000000000000\",\"created\":1624219079,\"miner\":\"dionyziz\",\"nonce\":\"0000000000000000000000000000000000000000000000000000002634878840\",\"note\":\"The Economist 2021-06-20: Crypto-miners are probably to blame for the graphics-chip shortage\",\"previd\":null,\"txids\":[],\"type\":\"block\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();

        // Send transaction
//        System.out.println("send tx");
//        request = "{\"object\": {" +
//                "\"type\" : \"transaction\" ," +
//                "\"height\" : 0," +
//                "\"outputs\" : [" +
//                "{" +
//                "\"pubkey\": \"8dbcd2401c89c04d6e53c81c90aa0b551cc8fc47c0469217c8f5cfbae1e911f9\" ," +
////                "\"value\" : 50000000000000" +
//                "\"value\" : 50000000000" +
//                "} ]" +
//                "}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();

        // Send block
//        System.out.println("send block");
//        request = "{\"object\": {" +
//                "\"T\":\"00000002af000000000000000000000000000000000000000000000000000000\" ," +
//                "\"created\":1624229079," +
//                "\"miner\":\"TUWien−Kerma\" ," +
//                "\"nonce\":\"200000000000000000000000000000000000000000000000000000000e762cb9\" ," +
//                "\"note\":\"First block . Yayy, I have 50 ker now!!\" ," +
//                "\"previd\":\"00000000a420b7cefa2b7730243316921ed59ffe836e111ca3801f82a4f5360e\" ," +
//                "\"txids\":[\"1bb37b637d07100cd26fc063dfd4c39a7931cc88dae3417871219715a5e374af\"] ," +
////                "\"txids\":[\"10ef61ebef8cc660b86bc6c338b379769e9cc3f768900a49f87999a32701bbea\"] ," +
//                "\"type\" :\"block\"" +
//                "}," +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();


        // cbtx_spent_in_same_block
//        System.out.println("send cbtx");
//        request = "{\"object\": " +
//                "{\"height\":1,\"outputs\":[{\"pubkey\":\"f66c7d51551d344b74e071d3b988d2bc09c3ffa82857302620d14f2469cfbf60\",\"value\":50000000000000}],\"type\":\"transaction\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        System.out.println("send tx");
//        request = "{\"object\": " +
//                "{\"inputs\":[{\"outpoint\":{\"index\":0,\"txid\":\"2a9458a2e75ed8bd0341b3cb2ab21015bbc13f21ea06229340a7b2b75720c4df\"},\"sig\":\"49cc4f9a1fb9d600a7debc99150e7909274c8c74edd7ca183626dfe49eb4aa21c6ff0e4c5f0dc2a328ad6b8ba10bf7169d5f42993a94bf67e13afa943b749c0b\"}],\"outputs\":[{\"pubkey\":\"c7c2c13afd02be7986dee0f4630df01abdbc950ea379055f1a423a6090f1b2b3\",\"value\":50}],\"type\":\"transaction\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        System.out.println("send invalid block");
//        request = "{\"object\": " +
//                "{\"T\":\"00000002af000000000000000000000000000000000000000000000000000000\",\"created\":1624220079,\"miner\":\"Snekel testminer\",\"nonce\":\"000000000000000000000000000000000000000000000000000000001beecbf3\",\"note\":\"First block after genesis with CBTX and TX spending it\",\"previd\":\"00000000a420b7cefa2b7730243316921ed59ffe836e111ca3801f82a4f5360e\",\"txids\":[\"2a9458a2e75ed8bd0341b3cb2ab21015bbc13f21ea06229340a7b2b75720c4df\",\"7ef80f2da40b3f681a5aeb7962731beddccea25fa51e6e7ae6fbce8a58dbe799\"],\"type\":\"block\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();

        // double_spend1 + double_spend_2
//        System.out.println("send cbtx");
//        request = "{\"object\": " +
//                "{\"height\":1,\"outputs\":[{\"pubkey\":\"f66c7d51551d344b74e071d3b988d2bc09c3ffa82857302620d14f2469cfbf60\",\"value\":50000000000000}],\"type\":\"transaction\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        System.out.println("send valid block");
//        request = "{\"object\": " +
//                "{\"T\":\"00000002af000000000000000000000000000000000000000000000000000000\",\"created\":1624220079,\"miner\":\"Snekel testminer\",\"nonce\":\"000000000000000000000000000000000000000000000000000000009d8b60ea\",\"note\":\"First block after genesis with CBTX\",\"previd\":\"00000000a420b7cefa2b7730243316921ed59ffe836e111ca3801f82a4f5360e\",\"txids\":[\"2a9458a2e75ed8bd0341b3cb2ab21015bbc13f21ea06229340a7b2b75720c4df\"],\"type\":\"block\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();

//        // double_spend_2
//        System.out.println("send cbtx");
//        request = "{\"object\": " +
//                "{\"height\":2,\"outputs\":[{\"pubkey\":\"c7c2c13afd02be7986dee0f4630df01abdbc950ea379055f1a423a6090f1b2b3\",\"value\":50000000000000}],\"type\":\"transaction\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        System.out.println("send valid block");
//        request = "{\"object\": " +
//                "{\"T\":\"00000002af000000000000000000000000000000000000000000000000000000\",\"created\":1624221079,\"miner\":\"Snekel testminer\",\"nonce\":\"000000000000000000000000000000000000000000000000000000004d82fc68\",\"note\":\"Second block after genesis with CBTX\",\"previd\":\"0000000108bdb42de5993bcf5f7d92557585dd6abfe9fb68e796518fe7f2ed2e\",\"txids\":[\"73231cc901774ddb4196ee7e9e6b857b208eea04aee26ced038ac465e1e706d2\"],\"type\":\"block\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();


        // double_spend_2 continued (invalid)
//        System.out.println("send tx");
//        request = "{\"object\": " +
//                "{\"inputs\":[{\"outpoint\":{\"index\":0,\"txid\":\"2a9458a2e75ed8bd0341b3cb2ab21015bbc13f21ea06229340a7b2b75720c4df\"},\"sig\":\"334939cac007a71e72484ffa5f34fabe3e3aff31297003a7d3d24795ed33d04a72f8b14316bce3e6467b2f6e66d481f8142ccd9933279fdcb3aef7ace145f10b\"},{\"outpoint\":{\"index\":0,\"txid\":\"73231cc901774ddb4196ee7e9e6b857b208eea04aee26ced038ac465e1e706d2\"},\"sig\":\"032c6c0a1074b7a965e58fa5071aa9e518bf5c4db9e2880ca5bb5c55dcea47cfd6e0a9859526a16d2bb0b46da0ca4c6f90be8ddf16b149be66016d7f272e6708\"}],\"outputs\":[{\"pubkey\":\"f66c7d51551d344b74e071d3b988d2bc09c3ffa82857302620d14f2469cfbf60\",\"value\":20}],\"type\":\"transaction\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        System.out.println("send tx");
//        request = "{\"object\": " +
//                "{\"inputs\":[{\"outpoint\":{\"index\":0,\"txid\":\"2a9458a2e75ed8bd0341b3cb2ab21015bbc13f21ea06229340a7b2b75720c4df\"},\"sig\":\"49cc4f9a1fb9d600a7debc99150e7909274c8c74edd7ca183626dfe49eb4aa21c6ff0e4c5f0dc2a328ad6b8ba10bf7169d5f42993a94bf67e13afa943b749c0b\"}],\"outputs\":[{\"pubkey\":\"c7c2c13afd02be7986dee0f4630df01abdbc950ea379055f1a423a6090f1b2b3\",\"value\":50}],\"type\":\"transaction\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        System.out.println("send invalid block");
//        request = "{\"object\": " +
//                "{\"T\":\"00000002af000000000000000000000000000000000000000000000000000000\",\"created\":1624222079,\"miner\":\"Snekel testminer\",\"nonce\":\"00000000000000000000000000000000000000000000000000000000062d431b\",\"note\":\"Third block after genesis with double-spending TX\",\"previd\":\"00000002a8986627f379547ed1ec990841e1f1c6ba616a56bfcd4b410280dc6d\",\"txids\":[\"fbb455506e5a7ce628fed88c8429e43810d3e306c4ff0c5a8313a1aeed6da88d\",\"7ef80f2da40b3f681a5aeb7962731beddccea25fa51e6e7ae6fbce8a58dbe799\"],\"type\":\"block\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();

        // double_spend_1 continued (valid)
//        System.out.println("send cbtx");
//        request = "{\"object\": " +
//                "{\"height\":2,\"outputs\":[{\"pubkey\":\"c7c2c13afd02be7986dee0f4630df01abdbc950ea379055f1a423a6090f1b2b3\",\"value\":50000000000000}],\"type\":\"transaction\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        System.out.println("send tx");
//        request = "{\"object\": " +
//                "{\"inputs\":[{\"outpoint\":{\"index\":0,\"txid\":\"2a9458a2e75ed8bd0341b3cb2ab21015bbc13f21ea06229340a7b2b75720c4df\"},\"sig\":\"49cc4f9a1fb9d600a7debc99150e7909274c8c74edd7ca183626dfe49eb4aa21c6ff0e4c5f0dc2a328ad6b8ba10bf7169d5f42993a94bf67e13afa943b749c0b\"}],\"outputs\":[{\"pubkey\":\"c7c2c13afd02be7986dee0f4630df01abdbc950ea379055f1a423a6090f1b2b3\",\"value\":50}],\"type\":\"transaction\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        System.out.println("send valid block");
//        request = "{\"object\": " +
//                "{\"T\":\"00000002af000000000000000000000000000000000000000000000000000000\",\"created\":1624221079,\"miner\":\"Snekel testminer\",\"nonce\":\"00000000000000000000000000000000000000000000000000000000182b95ea\",\"note\":\"Second block after genesis with CBTX and TX\",\"previd\":\"0000000108bdb42de5993bcf5f7d92557585dd6abfe9fb68e796518fe7f2ed2e\",\"txids\":[\"73231cc901774ddb4196ee7e9e6b857b208eea04aee26ced038ac465e1e706d2\",\"7ef80f2da40b3f681a5aeb7962731beddccea25fa51e6e7ae6fbce8a58dbe799\"],\"type\":\"block\"}, " +
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();


        // double_spend_1 continued more (invalid)
//        System.out.println("send tx");
//        request = "{\"object\": " +
//                "{\"inputs\":[{\"outpoint\":{\"index\":0,\"txid\":\"2a9458a2e75ed8bd0341b3cb2ab21015bbc13f21ea06229340a7b2b75720c4df\"},\"sig\":\"334939cac007a71e72484ffa5f34fabe3e3aff31297003a7d3d24795ed33d04a72f8b14316bce3e6467b2f6e66d481f8142ccd9933279fdcb3aef7ace145f10b\"},{\"outpoint\":{\"index\":0,\"txid\":\"73231cc901774ddb4196ee7e9e6b857b208eea04aee26ced038ac465e1e706d2\"},\"sig\":\"032c6c0a1074b7a965e58fa5071aa9e518bf5c4db9e2880ca5bb5c55dcea47cfd6e0a9859526a16d2bb0b46da0ca4c6f90be8ddf16b149be66016d7f272e6708\"}],\"outputs\":[{\"pubkey\":\"f66c7d51551d344b74e071d3b988d2bc09c3ffa82857302620d14f2469cfbf60\",\"value\":20}],\"type\":\"transaction\"}, "+
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        System.out.println("send invalid block");
//        request = "{\"object\": " +
//                "{\"T\":\"00000002af000000000000000000000000000000000000000000000000000000\",\"created\":1624222079,\"miner\":\"Snekel testminer\",\"nonce\":\"0000000000000000000000000000000000000000000000000000000010fea5cc\",\"note\":\"Third block after genesis with double-spending TX\",\"previd\":\"000000021dc4cfdcd0970084949f94da17f97504e1cc3e354851bb4768842b57\",\"txids\":[\"fbb455506e5a7ce628fed88c8429e43810d3e306c4ff0c5a8313a1aeed6da88d\"],\"type\":\"block\"}, "+
//                "\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();


        // errors from Task 2 rating:
//        System.out.println("send invalid tx");
//        request = "{\"object\":{\"inputs\":[{\"outpoint\":{\"index\":0,\"txid\":\"fe932d84ceea49b1ae1a3d2fecb01dd8520661f95d53d575c96451c272eee54d\"},\"sig\":\"2c15294b046147c2856a5ddc7b5397eaf6f776deee6cd3ad72b8a0201fe1cdc74f08cf88a10f558b9959238b3d9878ca79dd043ea66e94a040e8ea99f473e501\"},{\"outpoint\":{\"index\":0,\"txid\":\"86a6f22ce55e6835534987dd7ec0f810138b16e0d1eeb15f8c8a91c74b35c8a4\"},\"sig\":\"2c15294b046147c2856a5ddc7b5397eaf6f776deee6cd3ad72b8a0201fe1cdc74f08cf88a10f558b9959238b3d9878ca79dd043ea66e94a040e8ea99f473e501\"}],\"outputs\":[{\"pubkey\":\"fa12b5c9741c895ab0dce555f78bec5eb0b7b72a7136b065441dbe383bc805d8\",\"value\":5000000000011}],\"type\":\"transaction\"},\"type\":\"object\"}";
//        writer.println(request);
//        writer.flush();
//        response = reader.readLine(); // expect error object
//        System.out.println("Object: " + response);

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

        String host = "localhost";
//        String host = "139.59.159.65";
//        String host = "128.130.122.101";
        int port = 18018;
        testValidRequests(host, port);
//        testInvalidRequests(host, port);
//        System.out.println(isJson("{\"object\": {\"inputs\":[{\"outpoint\":{\"index\":0,\"txid\":\"48c2ae2fbb4dead4bcc5801f6eaa9a350123a43750d22d05c53802b69c7cd9fb\"},\"sig\":\"d51e82d5c121c5db21c83404aaa3f591f2099bccf731208c4b0b676308be1f994882f9d991c0ebfd8fdecc90a4aec6165fc3440ade9c83b043cba95b2bba1d0a\"}],\"outputs\":[{\"pubkey\":\"228ee807767047682e9a556ad1ed78dff8d7edf4bc2a5f4fa02e4634cfcad7e0\",\"value\":49000000000000}],\"type\":\"transaction\"}, " +
//                "\"type\":\"object\"}"));
    }
}
