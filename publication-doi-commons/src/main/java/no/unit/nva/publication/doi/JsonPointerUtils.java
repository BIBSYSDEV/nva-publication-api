package no.unit.nva.publication.doi;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;

public final class JsonPointerUtils {

    private JsonPointerUtils() {

    }

    public static String textFromNode(JsonNode jsonNode, JsonPointer jsonPointer) {
        JsonNode json = jsonNode.at(jsonPointer);
        return isPopulatedJsonPointer(json) ? json.asText() : null;
    }

    private static boolean isPopulatedJsonPointer(JsonNode json) {
        return !json.isNull() && !json.asText().isBlank();
    }
}
