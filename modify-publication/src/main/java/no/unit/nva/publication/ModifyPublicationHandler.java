package no.unit.nva.publication;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.unit.nva.Environment;
import no.unit.nva.GatewayResponse;
import no.unit.nva.model.Publication;
import no.unit.nva.model.util.ContextUtil;
import no.unit.nva.publication.service.ModifyResourceService;
import org.zalando.problem.Problem;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.Status;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static no.unit.nva.Logger.log;
import static no.unit.nva.Logger.logError;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.OK;

public class ModifyPublicationHandler implements RequestStreamHandler {

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String HEADERS_AUTHORIZATION = "/headers/Authorization";
    public static final String ALLOWED_ORIGIN_ENV = "ALLOWED_ORIGIN";
    public static final String API_HOST_ENV = "API_HOST";
    public static final String API_SCHEME_ENV = "API_SCHEME";
    public static final String ENVIRONMENT_VARIABLE_NOT_SET = "Environment variable not set: ";
    public static final String MISSING_AUTHORIZATION_IN_HEADERS = "Missing Authorization in Headers";
    public static final String MISSING_IDENTIFIER_IN_PATH_PARAMETERS = "Missing identifier in path parameters";
    public static final String PATH_PARAMETERS_IDENTIFIER = "/pathParameters/identifier";
    public static final String BODY = "/body";
    public static final String NOT_SAME_IDENTIFIERS = "Identifer in path parameter and body is not the same";
    public static final String PUBLICATION_CONTEXT_JSON = "publicationContext.json";

    public static final String APPLICATION_JSON = "application/json";
    public static final String CONTENT_TYPE = "Content-Type";

    private final transient String allowedOrigin;
    private final transient String apiHost;
    private final transient String apiScheme;
    private final transient ObjectMapper objectMapper;
    private final transient ModifyResourceService modifyResourceService;

    public ModifyPublicationHandler() {
        this(createObjectMapper(), new ModifyResourceService(), new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param objectMapper objectMapper
     * @param modifyResourceService    resourcePersistenceService
     * @param environment  environment
     */
    public ModifyPublicationHandler(ObjectMapper objectMapper, ModifyResourceService modifyResourceService,
                                    Environment environment) {
        this.objectMapper = objectMapper;
        this.modifyResourceService = modifyResourceService;
        this.allowedOrigin = environment.get(ALLOWED_ORIGIN_ENV)
                .orElseThrow(() -> new IllegalStateException(ENVIRONMENT_VARIABLE_NOT_SET + ALLOWED_ORIGIN_ENV));
        this.apiHost = environment.get(API_HOST_ENV)
                .orElseThrow(() -> new IllegalStateException(ENVIRONMENT_VARIABLE_NOT_SET + API_HOST_ENV));
        this.apiScheme = environment.get(API_SCHEME_ENV)
                .orElseThrow(() -> new IllegalStateException(ENVIRONMENT_VARIABLE_NOT_SET + API_SCHEME_ENV));
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        String authorization;
        UUID identifier;
        Publication publication;
        try {
            JsonNode event = objectMapper.readTree(input);
            authorization = Optional.ofNullable(event.at(HEADERS_AUTHORIZATION).textValue())
                    .orElseThrow(() -> new IllegalArgumentException(MISSING_AUTHORIZATION_IN_HEADERS));
            identifier = UUID.fromString(Optional.ofNullable(event.at(PATH_PARAMETERS_IDENTIFIER).textValue())
                    .orElseThrow(() -> new IllegalArgumentException(MISSING_IDENTIFIER_IN_PATH_PARAMETERS)));
            publication = objectMapper.readValue(event.at(BODY).textValue(), Publication.class);
        } catch (Exception e) {
            logError(e);
            writeErrorResponse(output, BAD_REQUEST, e);
            return;
        }

        log("Identifier in path parameters " + identifier.toString());
        log("Publication in request body " + objectMapper.writeValueAsString(publication));

        if (!publication.getIdentifier().equals(identifier)) {
            writeErrorResponse(output, BAD_REQUEST, NOT_SAME_IDENTIFIERS);
            return;
        }

        try {
            JsonNode publicationResponse =
                    modifyResourceService.modifyResource(identifier, publication, apiScheme, apiHost, authorization);
            getPublicationContext(PUBLICATION_CONTEXT_JSON).ifPresent(publicationContext -> {
                ContextUtil.injectContext(publicationResponse, publicationContext);
            });
            objectMapper.writeValue(output, new GatewayResponse<>(
                    objectMapper.writeValueAsString(publicationResponse), headers(), OK.getStatusCode()));
        } catch (IOException e) {
            logError(e);
            writeErrorResponse(output, BAD_GATEWAY, e);
        } catch (Exception e) {
            logError(e);
            writeErrorResponse(output, INTERNAL_SERVER_ERROR, e);
        }
    }

    private void writeErrorResponse(OutputStream output, Status status, String message) throws IOException {
        objectMapper.writeValue(output, new GatewayResponse<>(objectMapper.writeValueAsString(
                Problem.valueOf(status, message)), headers(), status.getStatusCode()));
    }

    private void writeErrorResponse(OutputStream output, Status status, Exception exception) throws IOException {
        writeErrorResponse(output, status, exception.getMessage());
    }

    protected Optional<JsonNode> getPublicationContext(String publicationContextPath) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(publicationContextPath)) {
            return Optional.of(objectMapper.readTree(inputStream));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Map<String,String> headers() {
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
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
