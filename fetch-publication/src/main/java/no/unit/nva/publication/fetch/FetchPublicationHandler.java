package no.unit.nva.publication.fetch;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import no.unit.nva.model.Publication;
import no.unit.nva.model.util.ContextUtil;
import no.unit.nva.publication.JsonLdContextUtil;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.impl.RestPublicationService;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.JsonUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchPublicationHandler extends ApiGatewayHandler<Void, JsonNode> {

    public static final String PUBLICATION_CONTEXT_JSON = "publicationContext.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(FetchPublicationHandler.class);
    private final PublicationService publicationService;
    private final ObjectMapper objectMapper;

    /**
     * Default constructor for MainHandler.
     */
    public FetchPublicationHandler() {
        this(new RestPublicationService(
                HttpClient.newHttpClient(),
                JsonUtils.objectMapper,
                new Environment()),
            JsonUtils.objectMapper,
            new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param publicationService publicationService
     * @param environment        environment
     */
    public FetchPublicationHandler(PublicationService publicationService,
                                   ObjectMapper objectMapper,
                                   Environment environment) {
        super(Void.class, environment);
        this.objectMapper = objectMapper;
        this.publicationService = publicationService;
        this.logger = LOGGER;
    }

    @Override
    protected JsonNode processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        Publication publication = publicationService.getPublication(
            RequestUtil.getIdentifier(requestInfo),
            RequestUtil.getAuthorization(requestInfo));

        return toJsonNodeWithContext(publication);
    }

    private JsonNode toJsonNodeWithContext(Publication publication) {
        JsonNode publicationJson = objectMapper.valueToTree(publication);
        new JsonLdContextUtil(objectMapper)
            .getPublicationContext(PUBLICATION_CONTEXT_JSON)
            .ifPresent(publicationContext -> ContextUtil.injectContext(publicationJson, publicationContext));
        return publicationJson;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, JsonNode output) {
        return HttpStatus.SC_OK;
    }
}
