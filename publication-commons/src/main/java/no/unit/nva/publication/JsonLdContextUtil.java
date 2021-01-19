package no.unit.nva.publication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

import nva.commons.core.ioutils.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonLdContextUtil {

    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(JsonLdContextUtil.class);

    public JsonLdContextUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Get PublicationContext as JsonNode.
     *
     * @param publicationContextPath publicationContextPath
     * @return optional publicationContext
     */
    public Optional<JsonNode> getPublicationContext(String publicationContextPath) {
        try (InputStream inputStream = IoUtils.inputStreamFromResources(Path.of(publicationContextPath))) {
            return Optional.of(objectMapper.readTree(inputStream));
        } catch (Exception e) {
            logger.warn("Error reading Publication Context: " + e.getMessage(), e);
            return Optional.empty();
        }
    }
}
