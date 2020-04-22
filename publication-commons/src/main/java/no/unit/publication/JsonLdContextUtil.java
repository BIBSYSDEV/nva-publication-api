package no.unit.publication;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Optional;

public class JsonLdContextUtil {

    private final ObjectMapper objectMapper;
    private final LambdaLogger logger;

    public JsonLdContextUtil(ObjectMapper objectMapper, LambdaLogger logger) {
        this.objectMapper = objectMapper;
        this.logger = logger;
    }

    public Optional<JsonNode> getPublicationContext(String publicationContextPath) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(publicationContextPath)) {
            return Optional.of(objectMapper.readTree(inputStream));
        } catch (Exception e) {
            logger.log("Error reading Publication Context: " + e.getMessage());
            return Optional.empty();
        }
    }

}
