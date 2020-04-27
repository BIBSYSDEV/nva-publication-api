package no.unit.nva.publication.modify;

import static nva.commons.utils.JsonUtils.objectMapper;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModifyPublicationHandler extends ApiGatewayHandler<Publication, JsonNode> {

    public static final String PUBLICATION_CONTEXT_JSON = "publicationContext.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyPublicationHandler.class);

    private final PublicationService publicationService;

    /**
     * Default constructor for MainHandler.
     */
    public ModifyPublicationHandler() {
        this(new RestPublicationService(
                HttpClient.newHttpClient(),
                objectMapper,
                new Environment()),
            new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param publicationService publicationService
     * @param environment        environment
     */
    public ModifyPublicationHandler(PublicationService publicationService,
                                    Environment environment) {
        super(Publication.class, environment);
        this.publicationService = publicationService;
        this.logger = LOGGER;
    }

    @Override
    protected JsonNode processInput(Publication input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        Publication publication = publicationService.updatePublication(
            RequestUtil.getIdentifier(requestInfo),
            input,
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
    protected Integer getSuccessStatusCode(Publication input, JsonNode output) {
        return HttpStatus.SC_OK;
    }
}