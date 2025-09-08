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
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.ValidatingApiGatewayHandler;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.commons.customer.CustomerNotAvailableException;
import no.unit.nva.publication.commons.customer.JavaHttpClientCustomerApiClient;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.rightsretention.RightsRetentionsApplier;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class CreatePublicationHandler
    extends ValidatingApiGatewayHandler<CreatePublicationRequest, PublicationResponse> {

    public static final String LOCATION_TEMPLATE = "%s://%s/publication/%s";
    public static final String API_SCHEME = "https";
    public static final String API_HOST = "API_HOST";
    private static final Logger logger = LoggerFactory.getLogger(CreatePublicationHandler.class);
    private static final List<String> THESIS_INSTANCE_TYPES = List.of("DegreeBachelor", "DegreeMaster", "DegreePhd",
                                                                      "ArtisticDegreePhd", "DegreeLicentiate",
                                                                      "OtherStudentWork");
    private final ResourceService publicationService;
    private final String apiHost;
    private final IdentityServiceClient identityServiceClient;
    private final SecretsReader secretsReader;
    private final HttpClient httpClient;
    private final JavaHttpClientCustomerApiClient customerApiClient;

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
        this.customerApiClient = getJavaHttpClientCustomerApiClient();
    }

    @Override
    protected void validateRequest(CreatePublicationRequest createPublicationRequest, RequestInfo requestInfo,
                                   Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected PublicationResponse processValidatedInput(CreatePublicationRequest input,
                                                        RequestInfo requestInfo,
                                                        Context context) throws ApiGatewayException {
        if (isThesisAndHasNoRightsToPublishThesisAndIsNotExternalClient(input, requestInfo)) {
            throw new ForbiddenException();
        }
        var newResource = Optional.ofNullable(input)
                                 .map(CreatePublicationRequest::toPublication)
                                 .map(Resource::fromPublication)
                                 .orElseGet(Resource::new);
        var customerAwareUserContext = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);
        var customer = fetchCustomerOrFailWithBadGateway(customerApiClient, customerAwareUserContext.getCustomerId());

        RightsRetentionsApplier.rrsApplierForNewPublication(newResource, customer.getRightsRetentionStrategy(),
                                                            customerAwareUserContext.getUsername()).handle();
        var createdPublication = newResource
                                     .persistNew(publicationService, customerAwareUserContext);
        setLocationHeader(createdPublication.getIdentifier());

        return PublicationResponseElevatedUser.fromPublication(createdPublication);
    }

    private JavaHttpClientCustomerApiClient getJavaHttpClientCustomerApiClient() {
        var backendClientCredentials = secretsReader.fetchClassSecret(
            environment.readEnv("BACKEND_CLIENT_SECRET_NAME"),
            BackendClientCredentials.class);
        var cognitoServerUri = URI.create(environment.readEnv("BACKEND_CLIENT_AUTH_URL"));
        var cognitoCredentials = new CognitoCredentials(backendClientCredentials::getId,
                                                        backendClientCredentials::getSecret,
                                                        cognitoServerUri);
        var authorizedBackendClient = AuthorizedBackendClient.prepareWithCognitoCredentials(httpClient,
                                                                                            cognitoCredentials);
        return new JavaHttpClientCustomerApiClient(authorizedBackendClient);
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

    @Override
    protected Integer getSuccessStatusCode(CreatePublicationRequest input, PublicationResponse output) {
        return HttpStatus.SC_CREATED;
    }

    protected URI getLocation(SortableIdentifier identifier) {
        return URI.create(String.format(LOCATION_TEMPLATE, API_SCHEME, apiHost, identifier));
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
}