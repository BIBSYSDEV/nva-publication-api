package no.unit.nva.publication.create;

import static nva.commons.utils.JsonUtils.objectMapper;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.impl.DynamoDBPublicationService;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.slf4j.LoggerFactory;

public class CreatePublicationHandler extends ApiGatewayHandler<CreatePublicationRequest, PublicationResponse> {

    public static final String LOCATION_TEMPLATE = "%s://%s/publication/%s";
    public static final String API_SCHEME = "API_SCHEME";
    public static final String API_HOST = "API_HOST";

    private final PublicationService publicationService;
    private final String apiScheme;
    private final String apiHost;

    /**
     * Default constructor for CreatePublicationHandler.
     */
    @JacocoGenerated
    public CreatePublicationHandler() {
        this(new DynamoDBPublicationService(
                AmazonDynamoDBClientBuilder.defaultClient(),
                objectMapper,
                new Environment()),
            new Environment());
    }

    /**
     * Constructor for CreatePublicationHandler.
     *
     * @param publicationService publicationService
     * @param environment        environment
     */
    public CreatePublicationHandler(PublicationService publicationService,
                                    Environment environment) {
        super(CreatePublicationRequest.class, environment, LoggerFactory.getLogger(CreatePublicationHandler.class));
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

    private void setLocationHeader(UUID identifier) {
        setAdditionalHeadersSupplier(() -> Map.of(
            HttpHeaders.LOCATION,
            getLocation(identifier).toString())
        );
    }

    protected URI getLocation(UUID identifier) {
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