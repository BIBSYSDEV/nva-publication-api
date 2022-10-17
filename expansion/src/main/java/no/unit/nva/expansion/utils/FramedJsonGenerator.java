package no.unit.nva.expansion.utils;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JacocoGenerated
public class FramedJsonGenerator {
    
    public static final String JSON_LD_GRAPH = "@graph";
    private static final Logger logger = LoggerFactory.getLogger(FramedJsonGenerator.class);
    private final Map<String, Object> framedJson;
    
    public FramedJsonGenerator(List<InputStream> streams, InputStream frame) {
        framedJson = attempt(() -> objectMapper.readValue(frame, Map.class))
                         .toOptional(fail -> logFramingFailure(fail.getException()))
                         .map(map -> createFramedJson(streams, map))
                         .orElseThrow();
    }
    
    public String getFramedJson() throws IOException {
        return com.github.jsonldjava.utils.JsonUtils.toPrettyString(framedJson);
    }
    
    private Map<String, Object> createFramedJson(List<InputStream> streams, Map<?, ?> frameMap) {
        return JsonLdProcessor.frame(createGraphDocumentFromInputStreams(streams),
            Objects.requireNonNull(frameMap), getDefaultOptions());
    }
    
    private Map<String, Object> createGraphDocumentFromInputStreams(List<InputStream> streams) {
        ObjectNode document = objectMapper.createObjectNode();
        ArrayNode graph = objectMapper.createArrayNode();
        streams.stream()
            .map(attempt(objectMapper::readTree))
            .filter(this::keepSuccessesAndLogErrors)
            .map(Try::orElseThrow)
            .forEach(graph::add);
        
        document.set(JSON_LD_GRAPH, graph);
        return objectMapper.convertValue(document, new TypeReference<>() {
        });
    }
    
    private boolean keepSuccessesAndLogErrors(Try<JsonNode> jsonNodeTry) {
        if (jsonNodeTry.isFailure()) {
            logFramingFailure(jsonNodeTry.getException());
        }
        return jsonNodeTry.isSuccess();
    }
    
    private void logFramingFailure(Exception exception) {
        logger.warn("Framing failed:", exception);
    }
    
    private JsonLdOptions getDefaultOptions() {
        JsonLdOptions options = new JsonLdOptions();
        options.setOmitGraph(true);
        options.setPruneBlankNodeIdentifiers(true);
        return options;
    }
}
