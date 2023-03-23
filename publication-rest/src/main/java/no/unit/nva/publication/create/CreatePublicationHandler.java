package no.unit.nva.publication.create;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.HttpHeaders;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePublicationHandler extends ApiGatewayHandler<CreatePublicationRequest, PublicationResponse> {
    
    public static final String LOCATION_TEMPLATE = "%s://%s/publication/%s";
    public static final String API_SCHEME = "https";
    public static final String API_HOST = "API_HOST";
    private static final Logger logger = LoggerFactory.getLogger(CreatePublicationHandler.class);
    private final ResourceService publicationService;
    private final String apiHost;
    private final IdentityServiceClient identityServiceClient;

    /**
     * Default constructor for CreatePublicationHandler.
     */
    @JacocoGenerated
    public CreatePublicationHandler() {
        this(new ResourceService(
                 AmazonDynamoDBClientBuilder.defaultClient(),
                 Clock.systemDefaultZone()),
             new Environment(),
             IdentityServiceClient.prepare());
    }
    
    /**
     * Constructor for CreatePublicationHandler.
     *
     * @param publicationService publicationService
     * @param environment        environment
     */
    public CreatePublicationHandler(ResourceService publicationService,
                                    Environment environment,
                                    IdentityServiceClient identityServiceClient) {
        super(CreatePublicationRequest.class, environment);
        this.publicationService = publicationService;
        this.apiHost = environment.readEnv(API_HOST);
        this.identityServiceClient = identityServiceClient;
    }
    
    @Override
    protected PublicationResponse processInput(CreatePublicationRequest input, RequestInfo requestInfo,
                                               Context context) throws ApiGatewayException {
        logger.info(attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(requestInfo)).orElseThrow());
        
        UserInstance userInstance = createUserInstanceFromLoginInformation(requestInfo);
        var newPublication = Optional.ofNullable(input)
                                 .map(CreatePublicationRequest::toPublication)
                                 .orElseGet(Publication::new);
        var createdPublication = Resource.fromPublication(newPublication).persistNew(publicationService, userInstance);
        setLocationHeader(createdPublication.getIdentifier());
    
        return PublicationResponse.fromPublication(createdPublication);
    }

    @Override
    protected Integer getSuccessStatusCode(CreatePublicationRequest input, PublicationResponse output) {
        return HttpStatus.SC_CREATED;
    }
    
    protected URI getLocation(SortableIdentifier identifier) {
        return URI.create(String.format(LOCATION_TEMPLATE, API_SCHEME, apiHost, identifier));
    }

    private UserInstance createUserInstanceForExternalClientUser(RequestInfo requestInfo) throws UnauthorizedException {
        var client = attempt(() -> requestInfo.getClientId().orElseThrow())
                         .map( clientId ->  identityServiceClient.getExternalClient(clientId))
                         .orElseThrow(fail -> new UnauthorizedException());

        var resourceOwner = new ResourceOwner(
            client.getActingUser(),
            client.getCristinUrgUri()
        );

        return UserInstance.create(resourceOwner, client.getCustomerUri());
    }

    private UserInstance createUserInstanceForInternalUser(RequestInfo requestInfo) throws UnauthorizedException {
        var resourceOwner = createInternalResourceOwner(requestInfo);
        var customerId = requestInfo.getCurrentCustomer();
        return UserInstance.create(resourceOwner, customerId);
    }

    private UserInstance createUserInstanceFromLoginInformation(RequestInfo requestInfo) throws UnauthorizedException {
        return requestInfo.clientIsThirdParty() ?
                   createUserInstanceForExternalClientUser(requestInfo)
                   : createUserInstanceForInternalUser(requestInfo);
    }
    
    private ResourceOwner createInternalResourceOwner(RequestInfo requestInfo) throws UnauthorizedException {
        return attempt(() -> requestInfo.getTopLevelOrgCristinId().orElseThrow())
                   .map(topLevelOrgCristinId -> new ResourceOwner(requestInfo.getNvaUsername(), topLevelOrgCristinId))
                   .orElseThrow(fail -> new UnauthorizedException());
    }
    
    private void setLocationHeader(SortableIdentifier identifier) {
        addAdditionalHeaders(() -> Map.of(
            HttpHeaders.LOCATION,
            getLocation(identifier).toString())
        );
    }
}