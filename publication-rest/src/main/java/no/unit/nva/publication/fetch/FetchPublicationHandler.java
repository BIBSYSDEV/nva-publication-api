package no.unit.nva.publication.fetch;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.model.DoiRequest;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import static no.unit.nva.publication.PublicationServiceConfig.EXTERNAL_SERVICES_HTTP_CLIENT;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractUserInstance;
import static nva.commons.core.attempt.Try.attempt;

public class FetchPublicationHandler extends ApiGatewayHandler<Void, String> {

    public static final Clock CLOCK = Clock.systemDefaultZone();
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
    protected String processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        var identifier = RequestUtil.getIdentifier(requestInfo);
        var publication = resourceService.getPublicationByIdentifier(identifier);
        var doiRequest = fetchDoiRequest(publication);
        publication.setDoiRequest(doiRequest);
        var publicationResponse = PublicationMapper
                .convertValue(publication, PublicationResponse.class);

        return attempt(() -> getObjectMapper(requestInfo).writeValueAsString(publicationResponse)).orElseThrow();
    }

    @Override
    protected Map<MediaType, ObjectMapper> getObjectMappers() {
        return Map.of(
                MediaType.XML_UTF_8, PublicationServiceConfig.xmlMapper
        );
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return List.of(
            MediaType.JSON_UTF_8,
            MediaTypes.APPLICATION_JSON_LD,
            MediaType.XML_UTF_8
        );
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpURLConnection.HTTP_OK;
    }

    @JacocoGenerated
    private static DoiRequestService defaultDoiRequestService(AmazonDynamoDB client) {
        return new DoiRequestService(client, EXTERNAL_SERVICES_HTTP_CLIENT, CLOCK);
    }

    @JacocoGenerated
    private static ResourceService defaultResourceService(AmazonDynamoDB client) {
        return new ResourceService(client, EXTERNAL_SERVICES_HTTP_CLIENT, CLOCK);
    }

    private DoiRequest fetchDoiRequest(Publication publication) {
        var owner = extractUserInstance(publication);
        var resourceIdentifier = publication.getIdentifier();
        return attempt(() -> doiRequestService.getDoiRequestByResourceIdentifier(owner, resourceIdentifier))
                   .map(no.unit.nva.publication.storage.model.DoiRequest::toPublication)
                   .map(Publication::getDoiRequest)
                   .orElse(fail -> null);
    }
}
