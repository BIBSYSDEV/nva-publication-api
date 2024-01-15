package no.unit.nva.publication.create;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.HttpHeaders;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.events.bodies.CreatePublicationRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.validation.ConfigNotAvailableException;
import no.unit.nva.publication.validation.config.CustomerApiFilesAllowedForTypesConfigSupplier;
import no.unit.nva.publication.validation.DefaultPublicationValidator;
import no.unit.nva.publication.validation.PublicationValidationException;
import no.unit.nva.publication.validation.PublicationValidator;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ForbiddenException;
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
    private static final List<String> THESIS_INSTANCE_TYPES = List.of("DegreeBachelor", "DegreeMaster", "DegreePhd");
    private final ResourceService publicationService;
    private final PublicationValidator publicationValidator;
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
             IdentityServiceClient.prepare(),
             new DefaultPublicationValidator(new CustomerApiFilesAllowedForTypesConfigSupplier()));
    }

    /**
     * Constructor for CreatePublicationHandler.
     *
     * @param publicationService publicationService
     * @param environment        environment
     */
    public CreatePublicationHandler(ResourceService publicationService,
                                    Environment environment,
                                    IdentityServiceClient identityServiceClient,
                                    PublicationValidator publicationValidator) {
        super(CreatePublicationRequest.class, environment);
        this.publicationService = publicationService;
        this.apiHost = environment.readEnv(API_HOST);
        this.identityServiceClient = identityServiceClient;
        this.publicationValidator = publicationValidator;
    }

    @Override
    protected PublicationResponse processInput(CreatePublicationRequest input, RequestInfo requestInfo,
                                               Context context) throws ApiGatewayException {
        logger.info(attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(requestInfo)).orElseThrow());
        if (isThesisAndHasNoRightsToPublishThesAndIsNotExternalClient(input, requestInfo)) {
            throw new ForbiddenException();
        }
        var newPublication = Optional.ofNullable(input)
                                 .map(CreatePublicationRequest::toPublication)
                                 .orElseGet(Publication::new);

        var customerAwareUserContext = getCustomerAwareUserContextFromLoginInformation(requestInfo);

        validatePublication(newPublication, customerAwareUserContext.customerUri());

        var createdPublication = Resource.fromPublication(newPublication)
                                     .persistNew(publicationService, customerAwareUserContext.userInstance());
        setLocationHeader(createdPublication.getIdentifier());

        return PublicationResponse.fromPublication(createdPublication);
    }

    private void validatePublication(Publication newPublication, URI customerUri)
        throws BadRequestException, BadGatewayException {
        try {
            publicationValidator.validate(newPublication, customerUri);
        } catch (PublicationValidationException e) {
            throw new BadRequestException(e.getMessage());
        } catch (ConfigNotAvailableException e) {
            logger.error("Failed to obtain config to perform validation", e);
            throw new BadGatewayException("Gateway not responding or not responding as expected!");
        }
    }

    @Override
    protected Integer getSuccessStatusCode(CreatePublicationRequest input, PublicationResponse output) {
        return HttpStatus.SC_CREATED;
    }

    protected URI getLocation(SortableIdentifier identifier) {
        return URI.create(String.format(LOCATION_TEMPLATE, API_SCHEME, apiHost, identifier));
    }

    private CustomerAwareUserContext getCustomerAwareUserContextFromLoginInformation(RequestInfo requestInfo)
        throws UnauthorizedException {
        return requestInfo.clientIsThirdParty()
                   ? customerAwareUserContextFromExternalClient(requestInfo, identityServiceClient)
                   : customerAwareUserContextFromInternalUser(requestInfo);
    }

    private static ResourceOwner createInternalResourceOwner(RequestInfo requestInfo) throws UnauthorizedException {
        return attempt(() -> requestInfo.getTopLevelOrgCristinId().orElseThrow())
                   .map(topLevelOrgCristinId -> new ResourceOwner(new Username(requestInfo.getUserName()),
                                                                  topLevelOrgCristinId))
                   .orElseThrow(fail -> new UnauthorizedException());
    }

    private void setLocationHeader(SortableIdentifier identifier) {
        addAdditionalHeaders(() -> Map.of(
            HttpHeaders.LOCATION,
            getLocation(identifier).toString())
        );
    }

    private boolean isThesisAndHasNoRightsToPublishThesAndIsNotExternalClient(CreatePublicationRequest request,
                                                                              RequestInfo requestInfo) {

        return isThesis(request) && !requestInfo.userIsAuthorized(MANAGE_DEGREE) && !requestInfo.clientIsThirdParty();
    }

    private boolean isThesis(CreatePublicationRequest request) {
        var requestInstanceType =
            attempt(() -> request.getEntityDescription().getReference().getPublicationInstance().getInstanceType())
                .toOptional();

        return requestInstanceType.isPresent() && THESIS_INSTANCE_TYPES.contains(requestInstanceType.get());
    }

    private CustomerAwareUserContext customerAwareUserContextFromExternalClient(
        final RequestInfo requestInfo,
        final IdentityServiceClient identityServiceClient) throws UnauthorizedException {

        var client = attempt(() -> requestInfo.getClientId().orElseThrow())
                         .map(identityServiceClient::getExternalClient)
                         .orElseThrow(fail -> new UnauthorizedException());

        final var customerUri = client.getCustomerUri();
        var resourceOwner = new ResourceOwner(
            new Username(client.getActingUser()),
            client.getCristinUrgUri()
        );

        final var userInstance = UserInstance.createExternalUser(resourceOwner, client.getCustomerUri());

        return new CustomerAwareUserContext(userInstance, customerUri);
    }

    private CustomerAwareUserContext customerAwareUserContextFromInternalUser(RequestInfo requestInfo)
        throws UnauthorizedException {
        var resourceOwner = createInternalResourceOwner(requestInfo);
        var customerUri = requestInfo.getCurrentCustomer();
        return new CustomerAwareUserContext(UserInstance.create(resourceOwner, customerUri), customerUri);
    }

    private record CustomerAwareUserContext(UserInstance userInstance, URI customerUri) {

    }
}