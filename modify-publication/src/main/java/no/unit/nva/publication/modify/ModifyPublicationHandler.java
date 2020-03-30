package no.unit.nva.publication.modify;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.publication.Environment;
import no.unit.publication.GatewayResponse;
import no.unit.publication.JacocoGenerated;
import no.unit.publication.PublicationHandler;
import no.unit.nva.model.Publication;
import no.unit.nva.model.util.ContextUtil;
import no.unit.publication.service.PublicationService;
import no.unit.publication.service.impl.RestPublicationService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpClient;
import java.util.Optional;
import java.util.UUID;

import static no.unit.publication.Logger.log;
import static no.unit.publication.Logger.logError;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.OK;

public class ModifyPublicationHandler extends PublicationHandler {

    public static final String BODY = "/body";
    public static final String HEADERS_AUTHORIZATION = "/headers/Authorization";
    public static final String PATH_PARAMETERS_IDENTIFIER = "/pathParameters/identifier";

    public static final String MISSING_AUTHORIZATION_IN_HEADERS =
            "Missing Authorization in Headers";
    public static final String MISSING_IDENTIFIER_IN_PATH_PARAMETERS = "Missing identifier in path parameters";
    public static final String NOT_SAME_IDENTIFIERS = "Identifier in path parameter and body is not the same";

    public static final String PUBLICATION_CONTEXT_JSON = "publicationContext.json";

    private final transient PublicationService publicationService;

    /**
     * Default constructor for MainHandler.
     */
    @JacocoGenerated
    public ModifyPublicationHandler() {
        this(PublicationHandler.createObjectMapper(),
                new RestPublicationService(
                        HttpClient.newHttpClient(),
                        new Environment()),
                new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param objectMapper objectMapper
     * @param publicationService    publicationService
     * @param environment  environment
     */
    public ModifyPublicationHandler(ObjectMapper objectMapper, PublicationService publicationService,
                                    Environment environment) {
        super(objectMapper, environment);
        this.publicationService = publicationService;
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
            Publication publicationResponse =
                    publicationService.updatePublication(publication, authorization);
            JsonNode publicationResponseJson = objectMapper.valueToTree(publicationResponse);
            getPublicationContext(PUBLICATION_CONTEXT_JSON).ifPresent(publicationContext -> {
                ContextUtil.injectContext(publicationResponseJson, publicationContext);
            });
            objectMapper.writeValue(output, new GatewayResponse<>(
                    objectMapper.writeValueAsString(publicationResponseJson), headers(), OK.getStatusCode()));
        } catch (IOException e) {
            logError(e);
            writeErrorResponse(output, BAD_GATEWAY, e);
        } catch (Exception e) {
            logError(e);
            writeErrorResponse(output, INTERNAL_SERVER_ERROR, e);
        }
    }
}
