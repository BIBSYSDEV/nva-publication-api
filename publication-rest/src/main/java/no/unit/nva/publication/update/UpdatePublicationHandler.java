package no.unit.nva.publication.update;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.*;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.exception.NotAuthorizedException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.RequestUtil.createExternalUserInstance;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_AUTH_URL;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_SECRET_NAME;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_RESOURCES;
import static nva.commons.apigateway.AccessRight.PUBLISH_THESIS;
import static nva.commons.core.attempt.Try.attempt;

@SuppressWarnings("PMD.GodClass")
public class UpdatePublicationHandler extends ApiGatewayHandler<UpdatePublicationRequest, PublicationResponse> {

    public static final String IDENTIFIER_MISMATCH_ERROR_MESSAGE = "Identifiers in path and in body, do not match";
    public static final String CONTENT_TYPE = "application/json";
    public static final String UNABLE_TO_FETCH_CUSTOMER_ERROR_MESSAGE = "Unable to fetch customer publishing workflow"
        + " from upstream";
    private static final Logger logger = LoggerFactory.getLogger(UpdatePublicationHandler.class);
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

    @Override
    protected PublicationResponse processInput(UpdatePublicationRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        SortableIdentifier identifierInPath = RequestUtil.getIdentifier(requestInfo);
        validateRequest(identifierInPath, input);
        Publication existingPublication = fetchExistingPublication(requestInfo, identifierInPath);
        Publication publicationUpdate = input.generatePublicationUpdate(existingPublication);
        if (isAlreadyPublished(existingPublication) && thereIsNoRelatedPendingPublishingRequest(publicationUpdate)) {
            createPublishingRequestOnFileUpdate(publicationUpdate);
        }
        Publication updatedPublication = resourceService.updatePublication(publicationUpdate);
        return PublicationResponse.fromPublication(updatedPublication);
    }

    @Override
    protected Integer getSuccessStatusCode(UpdatePublicationRequest input, PublicationResponse output) {
        return HttpStatus.SC_OK;
    }

    private static boolean isPending(TicketEntry publishingRequest) {
        return TicketStatus.PENDING.equals(publishingRequest.getStatus());
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
        var kind = attempt(() -> publication.getEntityDescription().getReference().getPublicationInstance())
            .toOptional();

        return kind.isPresent() &&
            (kind.get() instanceof DegreeBachelor
                || kind.get() instanceof DegreeMaster
                || kind.get() instanceof DegreePhd);
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

    private boolean userCanEditOtherPeoplesPublications(RequestInfo requestInfo) {

        var accessRight = EDIT_OWN_INSTITUTION_RESOURCES.name();
        return !requestInfo.clientIsThirdParty() && requestInfo.userIsAuthorized(accessRight);
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
        return publication.getResourceOwner().getOwner().equals(new Username(userInstance.getUsername()));
    }

    private boolean userUnauthorizedToPublishThesis(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(PUBLISH_THESIS.name());
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

        var userInstance = createUserInstanceFromRequest(requestInfo);
        var publication = fetchPublication(identifierInPath);
        var isOwner = userIsPublicationOwner(userInstance, publication);

        if (isThesis(publication) && (userUnauthorizedToPublishThesis(requestInfo) || !isOwner)) {
            logger.info("thesis={} unauthorized={} isOwner={}",
                isThesis(publication),
                userUnauthorizedToPublishThesis(requestInfo),
                isOwner);

            throw new ForbiddenException();
        }

        if (userCanEditOtherPeoplesPublications(requestInfo)) {
            checkUserIsInSameInstitutionAsThePublication(userInstance, publication);
            return publication;
        }
        if (userIsContributorWithUpdatingPublicationRights(requestInfo, publication)) {
            return publication;
        }
        if (isOwner) {
            return fetchPublicationForPublicationOwner(identifierInPath, userInstance);
        }
        throw new ForbiddenException();
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

    private void checkUserIsInSameInstitutionAsThePublication(UserInstance userInstance,
                                                              Publication existingPublication)
        throws NotAuthorizedException {
        if (!userInstance.getOrganizationUri().equals(getCustomerId(existingPublication))) {
            throw new NotAuthorizedException();
        }
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

}