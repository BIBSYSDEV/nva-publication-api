package no.unit.nva.publication.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.commons.json.JsonUtils;

public final class JsonLdFrameUtil {

    public static final String CONTEXT_KEY = "@context";
    public static final ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;

    private JsonLdFrameUtil() {
        // NO-OP
    }

    public static JsonNode from(String frame, String context) {
        var frameNode = asJsonNode(frame);
        var contextNode = asJsonNode(context);
        return from(frameNode, contextNode);
    }

    private static JsonNode from(JsonNode frameNode, JsonNode contextNode) {
        return ((ObjectNode) frameNode).set(CONTEXT_KEY, contextNode);
    }

    private static void validate(JsonNode node) {
        if (!node.isObject()) {
            throw new IllegalArgumentException();
        }
    }

    private static JsonNode asJsonNode(String json) {
        try {
            var node = MAPPER.readTree(json);
            validate(node);
            return node;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
