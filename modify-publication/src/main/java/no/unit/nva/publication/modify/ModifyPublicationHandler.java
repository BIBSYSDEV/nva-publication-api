package no.unit.nva.publication.modify;

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

public class ModifyPublicationHandler extends ApiGatewayHandler<Publication, JsonNode> {

    public static final String PUBLICATION_CONTEXT_JSON = "publicationContext.json";

    private final transient PublicationService publicationService;
    private final ObjectMapper objectMapper = ObjectMapperConfig.objectMapper;

    /**
     * Default constructor for MainHandler.
     */
    public ModifyPublicationHandler() {
        this(new RestPublicationService(
                        HttpClient.newHttpClient(),
                    ObjectMapperConfig.objectMapper,
                        new Environment()),
                new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param publicationService    publicationService
     * @param environment  environment
     */
    public ModifyPublicationHandler(PublicationService publicationService,
                                    Environment environment) {
        super(Publication.class, environment);
        this.publicationService = publicationService;
    }

    @Override
    protected JsonNode processInput(Publication input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        Publication publication = publicationService.updatePublication(
                RequestUtil.getIdentifier(requestInfo),
                input,
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
    protected Integer getSuccessStatusCode(Publication input, JsonNode output) {
        return HttpStatus.SC_OK;
    }
}
