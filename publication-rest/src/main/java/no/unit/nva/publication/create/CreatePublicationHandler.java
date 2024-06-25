package no.unit.nva.publication.create;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.HttpHeaders;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.commons.customer.CustomerNotAvailableException;
import no.unit.nva.publication.commons.customer.JavaHttpClientCustomerApiClient;
import no.unit.nva.publication.events.bodies.CreatePublicationRequest;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.rightsretention.RightsRetentionsApplier;
import no.unit.nva.publication.service.impl.ResourceService;
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
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class CreatePublicationHandler extends ApiGatewayHandler<CreatePublicationRequest, PublicationResponse> {

    public static final String LOCATION_TEMPLATE = "%s://%s/publication/%s";
    public static final String API_SCHEME = "https";
    public static final String API_HOST = "API_HOST";
    private static final Logger logger = LoggerFactory.getLogger(CreatePublicationHandler.class);
    private static final List<String> THESIS_INSTANCE_TYPES = List.of("DegreeBachelor", "DegreeMaster", "DegreePhd",
                                                                      "DegreeLicentiate");
    public static final String MULTIPLE_CRISTIN_IDENTIFIERS_MESSAGE = "Publication is not valid! "
                                                                      + "Multiple Cristin identifiers "
                                                                      + "are not allowed";
    public static final String SOMETHING_WENT_WRONG_MESSAGE = "An unknown exception occurred!";
    private final ResourceService publicationService;
    private final PublicationValidator publicationValidator;
    private final String apiHost;
    private final IdentityServiceClient identityServiceClient;
    private final SecretsReader secretsReader;
    private final HttpClient httpClient;
    private JavaHttpClientCustomerApiClient customerApiClient;

    /**
     * Default constructor for CreatePublicationHandler.
     */
    @JacocoGenerated
    public CreatePublicationHandler() {
        this(ResourceService.defaultService(),
             new Environment(),
             IdentityServiceClient.prepare(),
             SecretsReader.defaultSecretsManagerClient(),
             HttpClient.newHttpClient());
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
                                    SecretsManagerClient secretsManagerClient,
                                    HttpClient httpClient) {
        super(CreatePublicationRequest.class, environment);
        this.publicationService = publicationService;
        this.apiHost = environment.readEnv(API_HOST);
        this.identityServiceClient = identityServiceClient;
        this.secretsReader = new SecretsReader(secretsManagerClient);
        this.httpClient = httpClient;
        this.publicationValidator = new DefaultPublicationValidator();
        this.customerApiClient = getJavaHttpClientCustomerApiClient();
    }

    @Override
    protected PublicationResponse processInput(CreatePublicationRequest input, RequestInfo requestInfo,
                                               Context context) throws ApiGatewayException {
        if (isThesisAndHasNoRightsToPublishThesisAndIsNotExternalClient(input, requestInfo)) {
            throw new ForbiddenException();
        }
        var newPublication = Optional.ofNullable(input)
                                 .map(CreatePublicationRequest::toPublication)
                                 .orElseGet(Publication::new);
        var customerAwareUserContext = getCustomerAwareUserContextFromLoginInformation(requestInfo);
        var customer = fetchCustomerOrFailWithBadGateway(customerApiClient, customerAwareUserContext.customerUri());

        validatePublication(newPublication, customer);

        RightsRetentionsApplier.rrsApplierForNewPublication(newPublication, customer.getRightsRetentionStrategy(),
                                                            customerAwareUserContext.username()).handle();
        var createdPublication = Resource.fromPublication(newPublication)
                                     .persistNew(publicationService, customerAwareUserContext.userInstance());
        setLocationHeader(createdPublication.getIdentifier());

        return PublicationResponse.fromPublication(createdPublication);
    }


    private JavaHttpClientCustomerApiClient getJavaHttpClientCustomerApiClient() {
        var backendClientCredentials = secretsReader.fetchClassSecret(
            environment.readEnv("BACKEND_CLIENT_SECRET_NAME"),
            BackendClientCredentials.class);
        var cognitoServerUri = URI.create(environment.readEnv("BACKEND_CLIENT_AUTH_URL"));
        var cognitoCredentials = new CognitoCredentials(backendClientCredentials::getId,
                                                        backendClientCredentials::getSecret,
                                                        cognitoServerUri);
        return new JavaHttpClientCustomerApiClient(httpClient, cognitoCredentials);
    }

    private static Customer fetchCustomerOrFailWithBadGateway(CustomerApiClient customerApiClient,
                                                              URI customerUri) throws BadGatewayException {
        try {
            return customerApiClient.fetch(customerUri);
        } catch (CustomerNotAvailableException e) {
            logger.error("Problems fetching customer", e);
            throw new BadGatewayException("Customer API not responding or not responding as expected!");
        }
    }

    private void validatePublication(Publication newPublication, Customer customer) throws BadRequestException {
        try {
            publicationValidator.validate(newPublication, customer);
        } catch (PublicationValidationException e) {
            throw new BadRequestException(e.getMessage());
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

    private boolean isThesisAndHasNoRightsToPublishThesisAndIsNotExternalClient(CreatePublicationRequest request,
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

        return new CustomerAwareUserContext(userInstance, customerUri, client.getActingUser());
    }

    private static CustomerAwareUserContext customerAwareUserContextFromInternalUser(RequestInfo requestInfo)
        throws UnauthorizedException {
        var resourceOwner = createInternalResourceOwner(requestInfo);
        var customerUri = requestInfo.getCurrentCustomer();
        return fromUserInstance(UserInstance.create(resourceOwner, customerUri));
    }

    private static CustomerAwareUserContext fromUserInstance(UserInstance userInstance) {
        return new CustomerAwareUserContext(userInstance, userInstance.getCustomerId(), userInstance.getUsername());
    }

    private record CustomerAwareUserContext(UserInstance userInstance, URI customerUri, String username) {

    }
}