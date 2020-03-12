package no.unit.nva;

import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.zalando.problem.Problem;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.Status;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static no.unit.nva.Logger.logError;

public abstract class PublicationHandler implements RequestStreamHandler {

    protected final ObjectMapper objectMapper;
    protected final Environment environment;

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ALLOWED_ORIGIN_ENV = "ALLOWED_ORIGIN";
    public static final String ENVIRONMENT_VARIABLE_NOT_SET = "Environment variable not set: ";

    public static final String APPLICATION_JSON = "application/json";
    public static final String CONTENT_TYPE = "Content-Type";

    protected final transient String allowedOrigin;

    /**
     * Constructor for abstract PublicationHandler.
     *
     * @param objectMapper  objectMapper
     * @param environment   environment
     */
    public PublicationHandler(ObjectMapper objectMapper, Environment environment) {
        this.objectMapper = objectMapper;
        this.environment = environment;

        this.allowedOrigin = environment.get(ALLOWED_ORIGIN_ENV)
                .orElseThrow(() -> new IllegalStateException(ENVIRONMENT_VARIABLE_NOT_SET + ALLOWED_ORIGIN_ENV));
    }

    protected void writeErrorResponse(OutputStream output, Status status, String message) throws IOException {
        objectMapper.writeValue(output, new GatewayResponse<>(objectMapper.writeValueAsString(
                Problem.valueOf(status, message)), headers(), status.getStatusCode()));
    }

    protected void writeErrorResponse(OutputStream output, Status status, Exception exception) throws IOException {
        writeErrorResponse(output, status, exception.getMessage());
    }

    /**
     * Return the publication context file as JSON.
     *
     * @param publicationContextPath    publicationContextPath
     * @return  optional publication context as json
     */
    public Optional<JsonNode> getPublicationContext(String publicationContextPath) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(publicationContextPath)) {
            return Optional.of(objectMapper.readTree(inputStream));
        } catch (Exception e) {
            logError(e);
            return Optional.empty();
        }
    }

    protected Map<String,String> headers() {
        Map<String,String> headers = new ConcurrentHashMap<>();
        headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        headers.put(CONTENT_TYPE, APPLICATION_JSON);
        return headers;
    }

    /**
     * Create ObjectMapper.
     *
     * @return  objectMapper
     */
    public static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .registerModule(new ProblemModule())
                .registerModule(new JavaTimeModule())
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(Include.NON_NULL); // Ignore null fields
    }

}
