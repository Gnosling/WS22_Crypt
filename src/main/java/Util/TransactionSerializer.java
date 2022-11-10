package Util;

import Entities.Transaction;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;

import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;

public class TransactionSerializer extends JsonSerializer<Transaction> {

    @Override
    public void serialize(Transaction transaction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException, JsonProcessingException {
        jsonGenerator.writeStartObject();
        if (transaction.isCoinbase()) {
            jsonGenerator.writeObjectField("height", transaction.getHeight());
        } else {
            jsonGenerator.writeObjectField("inputs", transaction.getInputs());
        }
        jsonGenerator.writeObjectField("outputs", transaction.getOutputs());
        jsonGenerator.writeObjectField("type", transaction.getType());
        jsonGenerator.writeEndObject();
    }

    @Override
    public void serializeWithType(Transaction value, JsonGenerator gen,
                                  SerializerProvider provider, TypeSerializer typeSer)
            throws IOException, JsonProcessingException {

        serialize(value, gen, provider); // call your customized serialize method
    }
}
