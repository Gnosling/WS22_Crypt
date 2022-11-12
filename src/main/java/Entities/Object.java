package Entities;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;

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
    String toJson() throws JsonProcessingException;
    @Override
    boolean equals(java.lang.Object o);
}

//As.EXISTING_PROPERTY
