package no.unit.nva.publication.update;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.RequestUtil.createExternalUserInstance;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_AUTH_URL;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_SECRET_NAME;
import static nva.commons.apigateway.AccessRight.APPROVE_DOI_REQUEST;
import static nva.commons.apigateway.AccessRight.EDIT_ALL_NON_DEGREE_RESOURCES;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_RESOURCES;
import static nva.commons.apigateway.AccessRight.PUBLISH_DEGREE;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.commons.json.JsonUtils;
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
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.model.identityservice.CustomerPublishingWorkflowResponse;
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
import org.apache.http.HttpStatus;

@SuppressWarnings("PMD.GodClass")
public class UpdatePublicationHandler
    extends ApiGatewayHandler<UpdatePublicationRequest, PublicationResponseElevatedUser> {

    public static final String IDENTIFIER_MISMATCH_ERROR_MESSAGE = "Identifiers in path and in body, do not match";
    public static final String CONTENT_TYPE = "application/json";
    public static final String UNABLE_TO_FETCH_CUSTOMER_ERROR_MESSAGE = "Unable to fetch customer publishing workflow"
            + " from upstream";
    private final RawContentRetriever uriRetriever;
    private final TicketService ticketService;
    private final ResourceService resourceService;
    private final IdentityServiceClient identityServiceClient;

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
                new AuthorizedBackendUriRetriever(BACKEND_CLIENT_AUTH_URL, BACKEND_CLIENT_SECRET_NAME));
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
                                    RawContentRetriever uriRetriever) {
        super(UpdatePublicationRequest.class, environment);
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.identityServiceClient = identityServiceClient;
        this.uriRetriever = uriRetriever;
    }

    private static boolean isPending(TicketEntry publishingRequest) {
        return TicketStatus.PENDING.equals(publishingRequest.getStatus());
    }

    @Override
    protected PublicationResponseElevatedUser processInput(UpdatePublicationRequest input,
                                                           RequestInfo requestInfo,
                                                           Context context)
        throws ApiGatewayException {

        SortableIdentifier identifierInPath = RequestUtil.getIdentifier(requestInfo);
        validateRequest(identifierInPath, input);
        Publication existingPublication = fetchExistingPublication(requestInfo, identifierInPath);
        Publication publicationUpdate = input.generatePublicationUpdate(existingPublication);
        if (isAlreadyPublished(existingPublication) && thereIsNoRelatedPendingPublishingRequest(publicationUpdate)) {
            createPublishingRequestOnFileUpdate(publicationUpdate);
        }
        if (isAlreadyPublished(existingPublication) && thereIsNoFiles(publicationUpdate)) {
            autoCompletePendingPublishingRequestsIfNeeded(publicationUpdate);
        }
        Publication updatedPublication = resourceService.updatePublication(publicationUpdate);
        return PublicationResponseElevatedUser.fromPublication(updatedPublication);
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

    private boolean thereIsNoFiles(Publication publicationUpdate) {
        return publicationUpdate.getAssociatedArtifacts().stream()
                   .noneMatch(File.class::isInstance);
    }

    @Override
    protected Integer getSuccessStatusCode(UpdatePublicationRequest input, PublicationResponseElevatedUser output) {
        return HttpStatus.SC_OK;
    }

    private BadGatewayException createBadGatewayException() {
        return new BadGatewayException(UNABLE_TO_FETCH_CUSTOMER_ERROR_MESSAGE);
    }

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

        var accessRight = EDIT_OWN_INSTITUTION_RESOURCES.name();
        return !requestInfo.clientIsThirdParty() && requestInfo.userIsAuthorized(accessRight)
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
        return !requestInfo.userIsAuthorized(PUBLISH_DEGREE.name()) && !requestInfo.clientIsThirdParty();
    }

    private CustomerPublishingWorkflowResponse getCustomerPublishingWorkflowResponse(URI customerId)
            throws BadGatewayException {
        var response = uriRetriever.getRawContent(customerId, CONTENT_TYPE)
                .orElseThrow(this::createBadGatewayException);
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response,
                CustomerPublishingWorkflowResponse.class)).orElseThrow();
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
        return requestInfo.userIsAuthorized(EDIT_ALL_NON_DEGREE_RESOURCES.name());
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

    private PublishingRequestCase injectPublishingWorkflow(PublishingRequestCase ticket, URI customerId)
            throws BadGatewayException {
        var customerTransactionResult = getCustomerPublishingWorkflowResponse(customerId);
        ticket.setWorkflow(customerTransactionResult.convertToPublishingWorkflow());
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

    private void createPublishingRequestOnFileUpdate(Publication publicationUpdate) throws ApiGatewayException {
        if (containsNewPublishableFiles(publicationUpdate)) {
            attempt(() -> TicketEntry.requestNewTicket(publicationUpdate, PublishingRequestCase.class))
                    .map(publishingRequest -> injectPublishingWorkflow((PublishingRequestCase) publishingRequest,
                            getCustomerId(publicationUpdate)))
                    .map(publishingRequest -> publishingRequest.persistNewTicket(ticketService))
                    .orElseThrow(fail -> createBadGatewayException());
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
                        contributor.getAffiliations().stream().map(Organization::getId))
                .anyMatch(id ->
                        id.equals(userInstance.getOrganizationUri()));
    }

    private boolean userIsAuthorizedToApproveDoiRequest(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(APPROVE_DOI_REQUEST.name());
    }
}