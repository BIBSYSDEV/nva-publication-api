package no.unit.nva.publication.fetch;

import static nva.commons.core.JsonUtils.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.util.ContextUtil;
import no.unit.nva.publication.JsonLdContextUtil;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;
import org.slf4j.LoggerFactory;

public class FetchPublicationHandler extends ApiGatewayHandler<Void, JsonNode> {

    public static final String PUBLICATION_CONTEXT_JSON = "publicationContext.json";

    private final ResourceService resourceService;

    /**
     * Default constructor for MainHandler.
     */
    @JacocoGenerated
    public FetchPublicationHandler() {
        this(new ResourceService(
                AmazonDynamoDBClientBuilder.defaultClient(),
                Clock.systemDefaultZone()),
            new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param resourceService publicationService
     * @param environment     environment
     */
    public FetchPublicationHandler(ResourceService resourceService,
                                   Environment environment) {
        super(Void.class, environment, LoggerFactory.getLogger(FetchPublicationHandler.class));
        this.resourceService = resourceService;
    }

    @Override
    protected JsonNode processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {

        UserInstance userInstance = RequestUtil.extractUserInstance(requestInfo);
        String userInstanceString = attempt(() -> objectMapper.writeValueAsString(userInstance)).orElseThrow();

        String requestInfoString = attempt(() -> objectMapper.writeValueAsString(requestInfo)).orElseThrow();
        logger.info("User instance: " + userInstanceString);
        logger.info("Request info: " + requestInfoString);
        SortableIdentifier identifier = RequestUtil.getIdentifier(requestInfo);
        Publication publication = resourceService.getPublication(userInstance, identifier);
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
