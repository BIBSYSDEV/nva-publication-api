package no.unit.nva.publication.create;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.HttpHeaders;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class CreatePublicationHandler extends ApiGatewayHandler<CreatePublicationRequest, PublicationResponse> {

    public static final String LOCATION_TEMPLATE = "%s://%s/publication/%s";
    public static final String API_SCHEME = "API_SCHEME";
    public static final String API_HOST = "API_HOST";

    private final ResourceService publicationService;
    private final String apiScheme;
    private final String apiHost;

    /**
     * Default constructor for CreatePublicationHandler.
     */
    @JacocoGenerated
    public CreatePublicationHandler() {
        this(new ResourceService(
                AmazonDynamoDBClientBuilder.defaultClient(),
                Clock.systemDefaultZone()),
            new Environment());
    }

    /**
     * Constructor for CreatePublicationHandler.
     *
     * @param publicationService publicationService
     * @param environment        environment
     */
    public CreatePublicationHandler(ResourceService publicationService,
                                    Environment environment) {
        super(CreatePublicationRequest.class, environment);
        this.publicationService = publicationService;
        this.apiScheme = environment.readEnv(API_SCHEME);
        this.apiHost = environment.readEnv(API_HOST);
    }

    @Override
    protected PublicationResponse processInput(CreatePublicationRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        Publication newPublication = PublicationMapper.toNewPublication(
            input,
            RequestUtil.getOwner(requestInfo),
            null, //TODO: set handle
            null, //TODO: set link
            createPublisherFromCustomerId(RequestUtil.getCustomerId(requestInfo)));

        Publication createdPublication = publicationService.createPublication(newPublication);

        setLocationHeader(createdPublication.getIdentifier());

        return PublicationMapper.convertValue(createdPublication, PublicationResponse.class);
    }

    private void setLocationHeader(SortableIdentifier identifier) {
        addAdditionalHeaders(() -> Map.of(
            HttpHeaders.LOCATION,
            getLocation(identifier).toString())
        );
    }

    protected URI getLocation(SortableIdentifier identifier) {
        return URI.create(String.format(LOCATION_TEMPLATE, apiScheme, apiHost, identifier));
    }

    private Organization createPublisherFromCustomerId(URI customerId) {
        return new Builder()
            .withId(customerId)
            .build();
    }

    @Override
    protected Integer getSuccessStatusCode(CreatePublicationRequest input, PublicationResponse output) {
        return HttpStatus.SC_CREATED;
    }
}