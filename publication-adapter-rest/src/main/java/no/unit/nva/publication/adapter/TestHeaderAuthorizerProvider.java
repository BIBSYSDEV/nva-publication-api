package no.unit.nva.publication.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.http.Context;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TestHeaderAuthorizerProvider implements AuthorizerContextProvider {

    public static final String HEADER = "X-Adapter-Authorizer";
    private static final String CLAIMS_FIELD = "claims";
    private static final Logger logger = LoggerFactory.getLogger(TestHeaderAuthorizerProvider.class);

    private final ObjectMapper objectMapper;

    public TestHeaderAuthorizerProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ObjectNode> buildAuthorizerNode(Context ctx) {
        var headerValue = ctx.header(HEADER);
        if (headerValue == null || headerValue.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode parsed = objectMapper.readTree(headerValue);
            var authorizer = objectMapper.createObjectNode();
            var claims = parsed.has(CLAIMS_FIELD) && parsed.get(CLAIMS_FIELD).isObject()
                             ? parsed.get(CLAIMS_FIELD)
                             : parsed;
            authorizer.set(CLAIMS_FIELD, claims);
            return Optional.of(authorizer);
        } catch (Exception e) {
            logger.warn("Failed to parse {} header: {}", HEADER, e.getMessage());
            return Optional.empty();
        }
    }
}
