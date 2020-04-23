package no.unit.nva.publication;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.utils.IoUtils;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

public class JsonLdContextUtil {

    private final ObjectMapper objectMapper;
    private final LambdaLogger logger;

    public JsonLdContextUtil(ObjectMapper objectMapper, LambdaLogger logger) {
        this.objectMapper = objectMapper;
        this.logger = logger;
    }

    /**
     * Get PublicationContext as JsonNode.
     * @param publicationContextPath    publicationContextPath
     * @return  optional publicationContext
     */
    public Optional<JsonNode> getPublicationContext(String publicationContextPath) {
        try (InputStream inputStream = IoUtils.inputStreamFromResources(Path.of(publicationContextPath))) {
            return Optional.of(objectMapper.readTree(inputStream));
        } catch (Exception e) {
            logger.log("Error reading Publication Context: " + e.getMessage());
            return Optional.empty();
        }
    }

}
