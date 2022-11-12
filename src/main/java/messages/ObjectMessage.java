package messages;

import Entities.Object;
import Entities.Transaction;
import Util.TransactionSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

import java.util.List;

import static Util.Util.computeHash;

public class ObjectMessage {

    private String type;

    private Object object;

    public ObjectMessage() {}

    public ObjectMessage(String type, Object object) {
        this.type = type;
        this.object = object;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public static void main(String[] args) throws Exception{
        ObjectMapper objectMapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addSerializer(Transaction.class, new TransactionSerializer());
        objectMapper.registerModule(module);
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

        String request = "{\n" +
                "\"type\" : \"object\" ,\n" +
                "\"object\" : {\n" +
                "\"type\" : \"block\" ,\n" +
                "\"txids\" : [\"740bcfb434c89abe57bb2bc80290cd5495e87ebf8cd0dadb076bc50453590104\"],\n" +
                "\"nonce\" : \"a26d92800cf58e88a5ecf37156c031a4147c2128beeaf1cca2785c93242a4c8b\" ,\n" +
                "\"previd\" : \"0024839ec9632d382486ba7aac7e0bda3b4bda1d4bd79be9ae78e7e1e813ddd8\" ,\n" +
                "\"created\" :1622825642,\n" +
                "\"T\" : \"003a000000000000000000000000000000000000000000000000000000000000\",\n" +
                "\"miner\":\"*****\"," +
                "\"note\":\"A sample block\"" +
                "}\n" +
                "}";

//        request = "{\"object\":{\"inputs\":[{\"outpoint\":{\"index\":0,\n" +
//                "\"txid\":\"1bb37b637d07100cd26fc063dfd4c39a7931cc88dae3417871219715a5e374af\"},\n" +
//                "\"sig\":\"1d0d7d774042607c69a87ac5f1cdf92bf474c25fafcc089fe667602bfefb0494" +
//                "726c519e92266957429ced875256e6915eb8cea2ea66366e739415efc47a6805\"}],\n" +
//                "\"outputs\":[{\n" +
//                "\"pubkey\":\"8dbcd2401c89c04d6e53c81c90aa0b551cc8fc47c0469217c8f5cfbae1e911f9\",\n" +
//                "\"value\":10}],\"type\":\"transaction\"},\"type\":\"object\"}";
//
        request = "{\"object\":{\"height\":0,\"outputs\":[{\n" +
                "\"pubkey\":\"8dbcd2401c89c04d6e53c81c90aa0b551cc8fc47c0469217c8f5cfbae1e911f9\",\n" +
                "\"value\":50000000000}],\"type\":\"transaction\"},\"type\":\"object\"}\n";

//        request = "{\"height\":1,\"outputs\":[{\"pubkey\":\"62b7c521cd9211579cf70fd4099315643767b96711febaa5c76dc3daf27c281c\",\"value\":50000000000000}],\"type\":\"transaction\"}";


//        Object received = objectMapper.readValue(request, Object.class);
        ObjectMessage received = objectMapper.readValue(request, ObjectMessage.class);

        // TODO: object-id in hexstrings
        // TODO: For this homework, you may consider blocks and coinbase transactions to always be valid
        // TODO: the message to sign is the transaction body, with all null signatures
        // TODO: a transaction must have at least 1 input and 1 output, otherwise it is invalid.

        System.out.println(objectMapper.writeValueAsString(received));
//        System.out.println(objectMapper.writeValueAsString(received.getObject()));
        System.out.println(computeHash("{\"height\":1,\"outputs\":[{\"pubkey\":\"62b7c521cd9211579cf70fd4099315643767b96711febaa5c76dc3daf27c281c\",\"value\":50000000000000}],\"type\":\"transaction\"}"));
        System.out.println(computeHash("{\"inputs\":[{\"outpoint\":{\"index\":0,\"txid\":\"48c2ae2fbb4dead4bcc5801f6eaa9a350123a43750d22d05c53802b69c7cd9fb\"},\"sig\":\"d51e82d5c121c5db21c83404aaa3f591f2099bccf731208c4b0b676308be1f994882f9d991c0ebfd8fdecc90a4aec6165fc3440ade9c83b043cba95b2bba1d0a\"}],\"outputs\":[{\"pubkey\":\"228ee807767047682e9a556ad1ed78dff8d7edf4bc2a5f4fa02e4634cfcad7e0\",\"value\":49000000000000}],\"type\":\"transaction\"}"));
        System.out.println(computeHash("{\"T\":\"00000002af000000000000000000000000000000000000000000000000000000\",\"created\":1624219079,\"miner\":\"dionyziz\",\"nonce\":\"0000000000000000000000000000000000000000000000000000002634878840\",\"note\":\"The Economist 2021-06-20: Crypto-miners are probably to blame for the graphics-chip shortage\",\"previd\":null,\"txids\":[],\"type\":\"block\"}"));
        int i = 0;
    }
}
