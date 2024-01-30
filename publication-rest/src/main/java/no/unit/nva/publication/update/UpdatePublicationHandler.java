package no.unit.nva.publication.update;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.RequestUtil.createExternalUserInstance;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.commons.customer.CustomerNotAvailableException;
import no.unit.nva.publication.commons.customer.JavaHttpClientCustomerApiClient;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.validation.DefaultPublicationValidator;
import no.unit.nva.publication.validation.PublicationValidationException;
import no.unit.nva.publication.validation.PublicationValidator;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@SuppressWarnings("PMD.GodClass")
public class UpdatePublicationHandler
    extends ApiGatewayHandler<UpdatePublicationRequest, PublicationResponseElevatedUser> {

    private static final Logger logger = LoggerFactory.getLogger(UpdatePublicationHandler.class);

    public static final String IDENTIFIER_MISMATCH_ERROR_MESSAGE = "Identifiers in path and in body, do not match";
    private final TicketService ticketService;
    private final ResourceService resourceService;
    private final IdentityServiceClient identityServiceClient;
    private final SecretsReader secretsReader;
    private final HttpClient httpClient;
    private final PublicationValidator publicationValidator;

    /**
     * Default constructor for MainHandler.
     */
    @JacocoGenerated
    public UpdatePublicationHandler() {
        this(new ResourceService(
                 AmazonDynamoDBClientBuilder.defaultClient(),
                 Clock.systemDefaultZone()),
             TicketService.defaultService(),
             new Environment(),
             IdentityServiceClient.prepare(),
             SecretsReader.defaultSecretsManagerClient(),
             HttpClient.newHttpClient()
        );
    }

    /**
     * Constructor for MainHandler.
     *
     * @param resourceService publicationService
     * @param environment     environment
     */
    public UpdatePublicationHandler(ResourceService resourceService,
                                    TicketService ticketService,
                                    Environment environment,
                                    IdentityServiceClient identityServiceClient,
                                    SecretsManagerClient secretsManagerClient,
                                    HttpClient httpClient) {
        super(UpdatePublicationRequest.class, environment);
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.identityServiceClient = identityServiceClient;
        this.secretsReader = new SecretsReader(secretsManagerClient);
        this.publicationValidator = new DefaultPublicationValidator();
        this.httpClient = httpClient;
    }

    private static boolean isPending(TicketEntry publishingRequest) {
        return TicketStatus.PENDING.equals(publishingRequest.getStatus());
    }

    @Override
    protected PublicationResponseElevatedUser processInput(UpdatePublicationRequest input,
                                                           RequestInfo requestInfo,
                                                           Context context)
        throws ApiGatewayException {

        var identifierInPath = RequestUtil.getIdentifier(requestInfo);
        validateRequest(identifierInPath, input);

        var existingPublication = fetchExistingPublication(requestInfo, identifierInPath);
        var publicationUpdate = input.generatePublicationUpdate(existingPublication);

        var backendClientSecretName = environment.readEnv("BACKEND_CLIENT_SECRET_NAME");
        var backendClientCredentials = secretsReader.fetchClassSecret(backendClientSecretName,
                                                                      BackendClientCredentials.class);
        var cognitoServerUri = URI.create("https://" + environment.readEnv("BACKEND_CLIENT_AUTH_URL"));
        var cognitoCredentials = new CognitoCredentials(backendClientCredentials::getId,
                                                        backendClientCredentials::getSecret,
                                                        cognitoServerUri);
        var customerApiClient = new JavaHttpClientCustomerApiClient(httpClient, cognitoCredentials);

        var customer = fetchCustomerOrFailWithBadGateway(customerApiClient, publicationUpdate.getPublisher().getId());

        validatePublication(publicationUpdate, customer);

        if (isAlreadyPublished(existingPublication) && thereIsNoRelatedPendingPublishingRequest(publicationUpdate)) {
            createPublishingRequestOnFileUpdate(publicationUpdate, customer);
        }
        if (isAlreadyPublished(existingPublication) && thereAreNoFiles(publicationUpdate)) {
            autoCompletePendingPublishingRequestsIfNeeded(publicationUpdate);
        }

        Publication updatedPublication = resourceService.updatePublication(publicationUpdate);
        return PublicationResponseElevatedUser.fromPublication(updatedPublication);
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

    private void validatePublication(Publication publicationUpdate, Customer customer) throws BadRequestException {
        try {
            publicationValidator.validate(publicationUpdate, customer);
        } catch (PublicationValidationException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private void autoCompletePendingPublishingRequestsIfNeeded(Publication publication) {
        ticketService.fetchTicketsForUser(UserInstance.fromPublication(publication))
            .filter(PublishingRequestCase.class::isInstance)
            .filter(UpdatePublicationHandler::isPending)
            .map(PublishingRequestCase.class::cast)
            .forEach(ticket -> ticket.complete(publication, getOwner(publication)).persistUpdate(ticketService));
    }

    private static Username getOwner(Publication publication) {
        return publication.getResourceOwner().getOwner();
    }

    private boolean thereAreNoFiles(Publication publicationUpdate) {
        return publicationUpdate.getAssociatedArtifacts().stream()
                   .noneMatch(File.class::isInstance);
    }

    @Override
    protected Integer getSuccessStatusCode(UpdatePublicationRequest input, PublicationResponseElevatedUser output) {
        return HttpStatus.SC_OK;
    }

//    private BadGatewayException createBadGatewayException() {
//        return new BadGatewayException(UNABLE_TO_FETCH_CUSTOMER_ERROR_MESSAGE);
//    }
//
    private boolean containsNewPublishableFiles(Publication publicationUpdate) {
        var unpublishedFiles = getUnpublishedFiles(publicationUpdate);
        return !unpublishedFiles.isEmpty() && containsPublishableFile(unpublishedFiles);
    }

    private boolean containsPublishableFile(List<AssociatedArtifact> unpublishedFiles) {
        return unpublishedFiles.stream().anyMatch(this::isPublishable);
    }

    private boolean hasCristinId(Contributor contributor) {
        return nonNull(contributor.getIdentity()) && nonNull(contributor.getIdentity().getId());
    }

    private boolean hasMatchingIdentifier(Publication publication, TicketEntry ticketEntry) {
        return ticketEntry.getResourceIdentifier().equals(publication.getIdentifier());
    }

    private boolean identifiersDoNotMatch(SortableIdentifier identifierInPath,
                                          UpdatePublicationRequest input) {
        return !identifierInPath.equals(input.getIdentifier());
    }

    private boolean isAlreadyPublished(Publication existingPublication) {
        return PublicationStatus.PUBLISHED.equals(existingPublication.getStatus())
               || PublicationStatus.PUBLISHED_METADATA.equals(existingPublication.getStatus());
    }

    private boolean isPublishable(AssociatedArtifact artifact) {
        var file = (File) artifact;
        return nonNull(file.getLicense()) && !file.isAdministrativeAgreement();
    }

    private boolean isThesis(Publication publication) {
        return Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .map(this::isDegree)
                   .orElse(false);
    }

    private Boolean isDegree(PublicationInstance<? extends Pages> publicationInstance) {
        return publicationInstance instanceof DegreeBachelor
               || publicationInstance instanceof DegreeMaster
               || publicationInstance instanceof DegreePhd;
    }

    private boolean isUnpublishedFile(AssociatedArtifact artifact) {
        return artifact instanceof UnpublishedFile;
    }

    private boolean thereIsNoRelatedPendingPublishingRequest(Publication publication) {
        return ticketService.fetchTicketsForUser(UserInstance.fromPublication(publication))
                   .filter(PublishingRequestCase.class::isInstance)
                   .filter(ticketEntry -> hasMatchingIdentifier(publication, ticketEntry))
                   .filter(UpdatePublicationHandler::isPending)
                   .findAny()
                   .isEmpty();
    }

    private boolean userCanEditOtherPeoplesPublicationsInTheirOwnInstitution(RequestInfo requestInfo,
                                                                             UserInstance userInstance,
                                                                             Publication existingPublication) {

        return !requestInfo.clientIsThirdParty()
               && requestInfo.userIsAuthorized(AccessRight.MANAGE_RESOURCES_STANDARD)
               && userIsFromSameOrganizationAsPublication(userInstance, existingPublication);
    }

    private boolean userIsFromSameOrganizationAsPublication(UserInstance userInstance,
                                                            Publication existingPublication) {
        return userInstance.getOrganizationUri().equals(getCustomerId(existingPublication));
    }

    private boolean userIsContributor(URI cristinId, Publication publication) {
        return Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getContributors).stream()
                   .flatMap(Collection::stream)
                   .filter(this::hasCristinId)
                   .map(Contributor::getIdentity)
                   .map(Identity::getId)
                   .anyMatch(id -> attempt(() -> id.equals(cristinId)).orElseThrow());
    }

    private boolean userIsContributorWithUpdatingPublicationRights(RequestInfo requestInfo, Publication publication) {
        URI cristinId = attempt(requestInfo::getPersonCristinId).orElse(failure -> null);
        return nonNull(cristinId) && userIsContributor(cristinId, publication);
    }

    private boolean userIsPublicationOwner(UserInstance userInstance, Publication publication) {
        return getOwner(publication).equals(new Username(userInstance.getUsername()));
    }

    private boolean userUnauthorizedToPublishThesisAndIsNotExternalClient(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(MANAGE_DEGREE) && !requestInfo.clientIsThirdParty();
    }

    private List<AssociatedArtifact> getUnpublishedFiles(Publication publicationUpdate) {
        return publicationUpdate.getAssociatedArtifacts().stream()
                   .filter(this::isUnpublishedFile)
                   .collect(Collectors.toList());
    }

    private Publication fetchExistingPublication(RequestInfo requestInfo, SortableIdentifier identifierInPath)
        throws ApiGatewayException {

        var publication = fetchPublication(identifierInPath);
        return isThesis(publication)
                   ? getPublicationIfUserCanEditThesis(publication, requestInfo)
                   : getNonDegreePublication(publication, requestInfo);
    }

    private Publication getNonDegreePublication(Publication publication,
                                                RequestInfo requestInfo) throws ApiGatewayException {

        if (userCanEditAllNonDegreePublications(requestInfo)) {
            return publication;
        }
        if (userIsContributorWithUpdatingPublicationRights(requestInfo, publication)) {
            return publication;
        }
        var userInstance = createUserInstanceFromRequest(requestInfo);
        if (userCanEditOtherPeoplesPublicationsInTheirOwnInstitution(requestInfo, userInstance, publication)) {
            return publication;
        }
        if (userIsCuratorAndIsInSameInstitutionAsThePublicationContributor(publication, requestInfo, userInstance)) {
            return publication;
        }

        if (userIsPublicationOwner(userInstance, publication)) {
            return fetchPublicationForPublicationOwner(publication.getIdentifier(), userInstance);
        }
        throw new ForbiddenException();
    }

    private Publication getPublicationIfUserCanEditThesis(Publication publication,
                                                          RequestInfo requestInfo) throws ApiGatewayException {
        var userInstance = createUserInstanceFromRequest(requestInfo);
        if (userUnauthorizedToPublishThesisAndIsNotExternalClient(requestInfo)
            && !userIsPublicationOwner(userInstance, publication)) {
            throw new ForbiddenException();
        }
        return publication;
    }

    private boolean userCanEditAllNonDegreePublications(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(MANAGE_RESOURCES_ALL);
    }

    private Publication fetchPublication(SortableIdentifier identifierInPath) throws NotFoundException {
        return attempt(() -> resourceService.getPublicationByIdentifier(identifierInPath))
                   .orElseThrow(failure -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));
    }

    private Publication fetchPublicationForPublicationOwner(SortableIdentifier identifierInPath,
                                                            UserInstance userInstance)
        throws ApiGatewayException {
        return resourceService.getPublication(userInstance, identifierInPath);
    }

    private PublishingRequestCase injectPublishingWorkflow(PublishingRequestCase ticket, Customer customer) {
        ticket.setWorkflow(PublishingWorkflow.lookUp(customer.getPublicationWorkflow()));
        return ticket;
    }

    private URI getCustomerId(Publication publicationUpdate) {
        return publicationUpdate.getPublisher().getId();
    }

    private UserInstance createUserInstanceFromRequest(RequestInfo requestInfo) throws ApiGatewayException {
        return requestInfo.clientIsThirdParty()
                   ? createExternalUserInstance(requestInfo, identityServiceClient)
                   : extractUserInstance(requestInfo);
    }

    private UserInstance extractUserInstance(RequestInfo requestInfo) throws UnauthorizedException {
        return attempt(requestInfo::getCurrentCustomer)
                   .map(customerId -> UserInstance.create(requestInfo.getUserName(), customerId))
                   .orElseThrow(fail -> new UnauthorizedException());
    }

    private void createPublishingRequestOnFileUpdate(Publication publicationUpdate, Customer customer)
        throws ApiGatewayException {
        if (containsNewPublishableFiles(publicationUpdate)) {
            attempt(() -> TicketEntry.requestNewTicket(publicationUpdate, PublishingRequestCase.class))
                .map(publishingRequest -> injectPublishingWorkflow((PublishingRequestCase) publishingRequest,
                                                                   customer))
                .map(publishingRequest -> publishingRequest.persistNewTicket(ticketService));
                //.orElseThrow(fail -> createBadGatewayException());
        }
    }

    private void validateRequest(SortableIdentifier identifierInPath, UpdatePublicationRequest input)
        throws BadRequestException {
        if (identifiersDoNotMatch(identifierInPath, input)) {
            throw new BadRequestException(IDENTIFIER_MISMATCH_ERROR_MESSAGE);
        }
    }

    private boolean userIsCuratorAndIsInSameInstitutionAsThePublicationContributor(
        Publication existingPublication,
        RequestInfo requestInfo, UserInstance userInstance) {
        return userIsAuthorizedToApproveDoiRequest(requestInfo) && userAndContributorInTheSameInstitution(
            existingPublication, userInstance);
    }

    private boolean userAndContributorInTheSameInstitution(Publication publication,
                                                           UserInstance userInstance) {
        return publication.getEntityDescription().getContributors()
                   .stream().flatMap(contributor ->
                                         contributor.getAffiliations().stream()
                                             .filter(Organization.class::isInstance)
                                             .map(Organization.class::cast)
                                             .map(Organization::getId))
                   .anyMatch(id ->
                                 id.equals(userInstance.getOrganizationUri()));
    }

    private boolean userIsAuthorizedToApproveDoiRequest(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(MANAGE_DOI);
    }
}