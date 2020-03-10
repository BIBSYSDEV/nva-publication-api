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
import no.unit.nva.model.util.ContextUtil;
import no.unit.nva.publication.service.FetchResourceService;
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

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.NOT_FOUND;
import static org.zalando.problem.Status.OK;

public class FetchPublicationHandler implements RequestStreamHandler {

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String HEADERS_AUTHORIZATION = "/headers/Authorization";
    public static final String ALLOWED_ORIGIN_ENV = "ALLOWED_ORIGIN";
    public static final String API_HOST_ENV = "API_HOST";
    public static final String API_SCHEME_ENV = "API_SCHEME";
    public static final String ENVIRONMENT_VARIABLE_NOT_SET = "Environment variable not set: ";
    public static final String MISSING_AUTHORIZATION_IN_HEADERS = "Missing Authorization in Headers";
    public static final String MISSING_IDENTIFIER_IN_PATH_PARAMETERS = "Missing identifier in path parameters";
    public static final String PATH_PARAMETERS_IDENTIFIER = "/pathParameters/identifier";
    public static final String PUBLICATION_CONTEXT_JSON = "publicationContext.json";
    public static final String ITEMS_0 = "/Items/0";
    public static final String PUBLICATION_NOT_FOUND = "Publication not found.";

    private final transient String allowedOrigin;
    private final transient String apiHost;
    private final transient String apiScheme;
    private final transient ObjectMapper objectMapper;
    private final transient FetchResourceService fetchResourceService;

    public FetchPublicationHandler() {
        this(createObjectMapper(), new FetchResourceService(), new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param objectMapper objectMapper
     * @param fetchResourceService    resourcePersistenceService
     * @param environment  environment
     */
    public FetchPublicationHandler(ObjectMapper objectMapper, FetchResourceService fetchResourceService,
                                   Environment environment) {
        this.objectMapper = objectMapper;
        this.fetchResourceService = fetchResourceService;
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
        try {
            JsonNode event = objectMapper.readTree(input);
            authorization = Optional.ofNullable(event.at(HEADERS_AUTHORIZATION).textValue())
                    .orElseThrow(() -> new IllegalArgumentException(MISSING_AUTHORIZATION_IN_HEADERS));
            identifier = UUID.fromString(Optional.ofNullable(event.at(PATH_PARAMETERS_IDENTIFIER).textValue())
                    .orElseThrow(() -> new IllegalArgumentException(MISSING_IDENTIFIER_IN_PATH_PARAMETERS)));
        } catch (Exception e) {
            e.printStackTrace();
            writeErrorResponse(output, BAD_REQUEST, e);
            return;
        }

        log("Request for identifier: " + identifier.toString());

        try {
            JsonNode resource = fetchResourceService.fetchResource(identifier, apiScheme, apiHost, authorization);
            JsonNode publication = resource.at(ITEMS_0);

            if (publication.isMissingNode()) {
                writeErrorResponse(output, NOT_FOUND, PUBLICATION_NOT_FOUND);
                return;
            }

            getPublicationContext(PUBLICATION_CONTEXT_JSON).ifPresent(publicationContext -> {
                ContextUtil.injectContext(publication, publicationContext);
            });

            objectMapper.writeValue(output, new GatewayResponse<>(
                    objectMapper.writeValueAsString(publication), headers(), OK.getStatusCode()));
        } catch (IOException e) {
            e.printStackTrace();
            writeErrorResponse(output, BAD_GATEWAY, e);
        } catch (Exception e) {
            e.printStackTrace();
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
        headers.put(CONTENT_TYPE, APPLICATION_JSON.getMimeType());
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

    public static void log(String message) {
        System.out.println(message);
    }
}
