package no.unit.nva.publication.fetch;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.model.Publication;
import no.unit.nva.model.util.ContextUtil;
import no.unit.nva.publication.JsonLdContextUtil;
import no.unit.nva.publication.ObjectMapperConfig;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.impl.RestPublicationService;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import org.apache.http.HttpStatus;

import java.net.http.HttpClient;

public class FetchPublicationHandler extends ApiGatewayHandler<Void, JsonNode> {

    public static final String PUBLICATION_CONTEXT_JSON = "publicationContext.json";

    private final PublicationService publicationService;
    private final ObjectMapper objectMapper;

    /**
     * Default constructor for MainHandler.
     */
    public FetchPublicationHandler() {
        this(new RestPublicationService(
                    HttpClient.newHttpClient(),
                    ObjectMapperConfig.objectMapper,
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
        Publication publication = publicationService.getPublication(
                RequestUtil.getIdentifier(requestInfo),
                RequestUtil.getAuthorization(requestInfo));

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


}
