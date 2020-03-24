package no.unit.nva.fetch;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.Environment;
import no.unit.nva.GatewayResponse;
import no.unit.nva.PublicationHandler;
import no.unit.nva.model.Publication;
import no.unit.nva.model.util.ContextUtil;
import no.unit.nva.service.PublicationService;
import no.unit.nva.service.impl.RestPublicationService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpClient;
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

    public static final String MISSING_AUTHORIZATION_IN_HEADERS = "Missing Authorization in Headers";
    public static final String MISSING_IDENTIFIER_IN_PATH_PARAMETERS = "Missing identifier in path parameters";
    public static final String PUBLICATION_NOT_FOUND = "Publication not found.";

    public static final String PUBLICATION_CONTEXT_JSON = "publicationContext.json";

    private final transient PublicationService publicationService;

    public FetchPublicationHandler() {
        this(createObjectMapper(), RestPublicationService.create(HttpClient.newHttpClient(), new Environment()),
                new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param objectMapper objectMapper
     * @param publicationService    publicationService
     * @param environment  environment
     */
    public FetchPublicationHandler(ObjectMapper objectMapper, PublicationService publicationService,
                                   Environment environment) {
        super(objectMapper, environment);
        this.publicationService = publicationService;
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
            Optional<Publication> publication = publicationService.getPublication(identifier, authorization);

            if (publication.isPresent()) {
                JsonNode publicationJson = objectMapper.valueToTree(publication.get());

                getPublicationContext(PUBLICATION_CONTEXT_JSON).ifPresent(publicationContext -> {
                    ContextUtil.injectContext(publicationJson, publicationContext);
                });

                objectMapper.writeValue(output, new GatewayResponse<>(
                        objectMapper.writeValueAsString(publicationJson), headers(), OK.getStatusCode()));
            } else {
                writeErrorResponse(output, NOT_FOUND, PUBLICATION_NOT_FOUND);
            }
        } catch (IOException e) {
            logError(e);
            writeErrorResponse(output, BAD_GATEWAY, e);
        } catch (Exception e) {
            logError(e);
            writeErrorResponse(output, INTERNAL_SERVER_ERROR, e);
        }
    }
}
