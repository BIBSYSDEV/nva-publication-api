package no.unit.nva.publication;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.Environment;
import no.unit.nva.GatewayResponse;
import no.unit.nva.PublicationHandler;
import no.unit.nva.model.util.ContextUtil;
import no.unit.nva.publication.service.FetchResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.UUID;

import static no.unit.nva.Logger.log;
import static no.unit.nva.Logger.logError;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.NOT_FOUND;
import static org.zalando.problem.Status.OK;

public class FetchPublicationHandler extends PublicationHandler {

    public static final String HEADERS_AUTHORIZATION = "/headers/Authorization";
    public static final String PATH_PARAMETERS_IDENTIFIER = "/pathParameters/identifier";
    public static final String ITEMS_0 = "/Items/0";


    public static final String API_HOST_ENV = "API_HOST";
    public static final String API_SCHEME_ENV = "API_SCHEME";

    public static final String ENVIRONMENT_VARIABLE_NOT_SET = "Environment variable not set: ";
    public static final String MISSING_AUTHORIZATION_IN_HEADERS = "Missing Authorization in Headers";
    public static final String MISSING_IDENTIFIER_IN_PATH_PARAMETERS = "Missing identifier in path parameters";
    public static final String PUBLICATION_NOT_FOUND = "Publication not found.";

    public static final String PUBLICATION_CONTEXT_JSON = "publicationContext.json";

    private final transient String apiHost;
    private final transient String apiScheme;
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
        super(objectMapper, environment);
        this.fetchResourceService = fetchResourceService;
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
            logError(e);
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
            logError(e);
            writeErrorResponse(output, BAD_GATEWAY, e);
        } catch (Exception e) {
            logError(e);
            writeErrorResponse(output, INTERNAL_SERVER_ERROR, e);
        }
    }
}
