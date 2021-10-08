package no.unit.nva.publication.fetch;

import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractOwner;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.time.Clock;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequest;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class FetchPublicationHandler extends ApiGatewayHandler<Void, PublicationResponse> {

    public static final String PUBLICATION_CONTEXT_JSON = "publicationContext.json";

    private final ResourceService resourceService;
    private final DoiRequestService doiRequestService;

    @JacocoGenerated
    public FetchPublicationHandler() {
        this(AmazonDynamoDBClientBuilder.defaultClient());
    }

    @JacocoGenerated
    public FetchPublicationHandler(AmazonDynamoDB client) {
        this(defaultResourceService(client),
             defaultDoiRequestService(client),
             new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param resourceService publicationService
     * @param environment     environment
     */
    public FetchPublicationHandler(ResourceService resourceService,
                                   DoiRequestService doiRequestService,
                                   Environment environment) {
        super(Void.class, environment);
        this.resourceService = resourceService;
        this.doiRequestService = doiRequestService;
    }

    @Override
    protected PublicationResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        SortableIdentifier identifier = RequestUtil.getIdentifier(requestInfo);
        Publication publication = resourceService.getPublicationByIdentifier(identifier);
        DoiRequest doiRequest = fetchDoiRequest(publication);
        publication.setDoiRequest(doiRequest);
        return PublicationMapper.convertValue(publication, PublicationResponse.class);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PublicationResponse output) {
        return HttpURLConnection.HTTP_OK;
    }

    @JacocoGenerated
    private static DoiRequestService defaultDoiRequestService(AmazonDynamoDB client) {
        return new DoiRequestService(client, Clock.systemDefaultZone());
    }

    @JacocoGenerated
    private static ResourceService defaultResourceService(AmazonDynamoDB client) {
        return new ResourceService(client, Clock.systemDefaultZone());
    }

    private DoiRequest fetchDoiRequest(Publication publication) {
        UserInstance owner = extractOwner(publication);
        SortableIdentifier resourceIdentifier = publication.getIdentifier();
        return attempt(() -> doiRequestService.getDoiRequestByResourceIdentifier(owner, resourceIdentifier))
                   .map(no.unit.nva.publication.storage.model.DoiRequest::toPublication)
                   .map(Publication::getDoiRequest)
                   .orElse(fail -> null);
    }
}
