package no.unit.nva.publication.fetch;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.model.Publication;
import no.unit.nva.model.util.ContextUtil;
import no.unit.publication.JsonLdContextUtil;
import no.unit.publication.ObjectMapperConfig;
import no.unit.publication.exception.InputException;
import no.unit.publication.service.PublicationService;
import no.unit.publication.service.impl.RestPublicationService;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import java.net.http.HttpClient;
import java.util.UUID;

public class FetchPublicationHandler extends ApiGatewayHandler<Void, JsonNode> {

    public static final String IDENTIFIER = "identifier";
    public static final String MISSING_AUTHORIZATION_IN_HEADERS = "Missing Authorization in Headers";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";
    public static final String PUBLICATION_CONTEXT_JSON = "publicationContext.json";

    private final PublicationService publicationService;
    private final ObjectMapper objectMapper;

    /**
     * Default constructor for MainHandler.
     */
    public FetchPublicationHandler() {
        this(new RestPublicationService(
                    HttpClient.newHttpClient(),
                    new Environment()),
                ObjectMapperConfig.objectMapper,
                new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param publicationService    publicationService
     * @param environment  environment
     */
    public FetchPublicationHandler(PublicationService publicationService,
                                   ObjectMapper objectMapper,
                                   Environment environment) {
        super(Void.class, environment);
        this.objectMapper = objectMapper;
        this.publicationService = publicationService;
    }

    @Override
    protected JsonNode processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        UUID identifier = getIdentifier(requestInfo);
        logger.log("Request for identifier: " + identifier.toString());
        Publication publication = publicationService.getPublication(identifier, getAuthorization(requestInfo));

        JsonNode publicationJson = objectMapper.valueToTree(publication);
        addContext(publicationJson);
        return publicationJson;
    }

    private void addContext(JsonNode publicationJson) {
        new JsonLdContextUtil(objectMapper, logger)
                .getPublicationContext(PUBLICATION_CONTEXT_JSON)
                .ifPresent(publicationContext -> ContextUtil.injectContext(publicationJson, publicationContext));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, JsonNode output) {
        return HttpStatus.SC_OK;
    }

    protected String getAuthorization(RequestInfo requestInfo) throws ApiGatewayException {
        try {
            return requestInfo.getHeaders().get(HttpHeaders.AUTHORIZATION);
        } catch (Exception e) {
            throw new InputException(MISSING_AUTHORIZATION_IN_HEADERS, e);
        }
    }

    protected UUID getIdentifier(RequestInfo requestInfo) throws ApiGatewayException {
        String identifier = null;
        try {
            identifier = requestInfo.getPathParameters().get(IDENTIFIER);
            return UUID.fromString(identifier);
        } catch (Exception e) {
            throw new InputException(IDENTIFIER_IS_NOT_A_VALID_UUID + identifier, e);
        }
    }
}
