package Entities;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Block.class, name = "block"),
        @JsonSubTypes.Type(value = Transaction.class, name = "transaction")
})
public interface Object {

    boolean verifyObject();
    void setType(String block);
    String getType();
}

//As.EXISTING_PROPERTY
