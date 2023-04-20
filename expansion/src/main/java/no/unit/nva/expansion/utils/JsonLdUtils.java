package no.unit.nva.expansion.utils;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.JsonNode;

public final class JsonLdUtils {
    
    private JsonLdUtils() {
    
    }
    
    public static String toJsonString(JsonNode root) {
        return attempt(() -> objectMapper.writeValueAsString(root)).orElseThrow();
    }
}
