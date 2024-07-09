package no.unit.nva.model.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ContextUtilTest {

    @DisplayName("ContextUtil.injectContext throws IllegalArgumentException when the input JsonNode is not an object")
    @Test
    public void injectContextThrowsIllegalArgumentExceptionWhenJsonNodeIsNotAnObject() {

        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        ObjectNode objectNode = objectMapper.createObjectNode();

        Assertions.assertThrows(IllegalArgumentException.class, () -> ContextUtil.injectContext(arrayNode, objectNode),
                ContextUtil.ERROR_MESSAGE);

    }

}
