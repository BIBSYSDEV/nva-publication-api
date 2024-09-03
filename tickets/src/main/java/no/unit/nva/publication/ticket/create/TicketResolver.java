package no.unit.nva.publication.ticket.create;

import static no.unit.nva.model.PublicationOperation.DOI_REQUEST_CREATE;
import static no.unit.nva.model.PublicationOperation.PUBLISHING_REQUEST_CREATE;
import static no.unit.nva.model.PublicationOperation.SUPPORT_REQUEST_CREATE;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_AUTH_URL;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_SECRET_NAME;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.AdministrativeAgreement;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.PublicationPermissionStrategy;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.DoiRequestDto;
import no.unit.nva.publication.ticket.GeneralSupportRequestDto;
import no.unit.nva.publication.ticket.PublishingRequestDto;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.UnpublishRequestDto;
import no.unit.nva.publication.ticket.model.identityservice.CustomerPublishingWorkflowResponse;
import no.unit.nva.publication.utils.RequestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TicketResolver {

    public static final String CONTENT_TYPE = "application/json";
    private final Logger logger = LoggerFactory.getLogger(TicketResolver.class);
    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final RawContentRetriever uriRetriever;

    @JacocoGenerated
    public TicketResolver() {
        this(ResourceService.defaultService(), TicketService.defaultService(),
             new AuthorizedBackendUriRetriever(BACKEND_CLIENT_SECRET_NAME, BACKEND_CLIENT_AUTH_URL));
    }

    public TicketResolver(ResourceService resourceService, TicketService ticketService,
                          RawContentRetriever uriRetriever) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.uriRetriever = uriRetriever;
    }

    public TicketEntry resolveAndPersistTicket(TicketDto ticketDto, RequestUtils requestUtils)
        throws ApiGatewayException {
        var publication = fetchPublication(requestUtils);
        var permissionStrategy = PublicationPermissionStrategy.create(publication, requestUtils.toUserInstance(), resourceService);

        validateUserPermissions(permissionStrategy, ticketDto);

        var ticket = TicketEntry.requestNewTicket(publication, ticketDto.ticketType());
        ticket.setOwnerAffiliation(requestUtils.topLevelCristinOrgId());
        if (isPublishingRequestCase(ticket)) {
            var customerId = requestUtils.customerId();
            var publishingRequestCase = updatePublishingRequestWorkflow((PublishingRequestCase) ticket, customerId);
            return createPublishingRequest(publishingRequestCase, publication, requestUtils);
        }
        return persistTicket(ticket);
    }

    private static boolean userDoesNotHavePermissionToCreateTicket(PublicationPermissionStrategy permissionStrategy,
                                                                   TicketDto ticketDto) {
        return switch (ticketDto) {
            case DoiRequestDto ignored -> permissionStrategy.allowsAction(DOI_REQUEST_CREATE);
            case PublishingRequestDto ignored -> permissionStrategy.allowsAction(PUBLISHING_REQUEST_CREATE);
            case GeneralSupportRequestDto ignored -> permissionStrategy.allowsAction(SUPPORT_REQUEST_CREATE);
            case null, default -> false;
        };
    }

    private static boolean hasValidAccessRights(RequestUtils requestUtils, TicketDto ticketDto) {
        return switch (ticketDto) {
            case DoiRequestDto ignored -> requestUtils.hasAccessRight(MANAGE_DOI);
            case PublishingRequestDto ignored -> requestUtils.hasAccessRight(MANAGE_PUBLISHING_REQUESTS);
            case GeneralSupportRequestDto ignored -> requestUtils.hasAccessRight(SUPPORT);
            case UnpublishRequestDto ignored -> requestUtils.hasAccessRight(MANAGE_PUBLISHING_REQUESTS);
            case null, default -> false;
        };
    }

    private static URI getPublisherId(Publication publication) {
        return Optional.ofNullable(publication.getPublisher()).map(Organization::getId).orElse(null);
    }

    private static boolean isNotAdministrativeAgreement(AssociatedArtifact artifact) {
        return artifact instanceof File && !(artifact instanceof AdministrativeAgreement);
    }

    private static boolean hasNoFiles(Publication publication) {
        return publication.getAssociatedArtifacts().stream().noneMatch(File.class::isInstance);
    }

    private void validateUserPermissions(PublicationPermissionStrategy permissionStrategy, TicketDto ticketDto)
        throws ForbiddenException {
        if (userDoesNotHavePermissionToCreateTicket(permissionStrategy, ticketDto)) {
            throw new ForbiddenException();
        }
    }

    private Publication fetchPublication(RequestUtils requestUtils) throws ApiGatewayException {
        return attempt(requestUtils::publicationIdentifier).map(resourceService::getPublicationByIdentifier)
                   .orElseThrow(fail -> loggingFailureReporter(fail.getException()));
    }

    private boolean userIsAuthorized(RequestUtils requestUtils, Publication publication, TicketDto ticketDto) {
        return hasValidAccessRightAtCustomer(requestUtils, publication, ticketDto) ||
               userTopLevelOrganizationIsCurationInstitution(requestUtils, publication);
    }

    private boolean hasValidAccessRightAtCustomer(RequestUtils requestUtils, Publication publication,
                                                  TicketDto ticketDto) {
        return hasValidAccessRights(requestUtils, ticketDto) && matchingCustomer(requestUtils, publication);
    }

    private boolean userTopLevelOrganizationIsCurationInstitution(RequestUtils requestUtils, Publication publication) {
        var userTopLevelCristinOrganizationId = requestUtils.topLevelCristinOrgId();
        return publication.getCuratingInstitutions().stream().anyMatch(userTopLevelCristinOrganizationId::equals);
    }

    private boolean matchingCustomer(RequestUtils requestUtils, Publication publication) {
        return Optional.ofNullable(requestUtils.customerId())
                   .map(customer -> customer.equals(getPublisherId(publication)))
                   .orElse(false);
    }

    private ApiGatewayException loggingFailureReporter(Exception exception) {
        logger.error("Request failed: {}", Arrays.toString(exception.getStackTrace()));
        return new ForbiddenException();
    }

    private PublishingRequestCase createPublishingRequest(PublishingRequestCase publishingRequestCase,
                                                          Publication publication, RequestUtils requestUtils)
        throws ApiGatewayException {
        var username = new Username(requestUtils.username());
        return requestUtils.isAuthorizedToManage(publishingRequestCase) ? createPublishingRequestForCurator(
            publishingRequestCase, publication, username)
                   : createPublishingRequestForNonCurator(publishingRequestCase, publication, username);
    }

    private PublishingRequestCase createPublishingRequestForCurator(PublishingRequestCase publishingRequestCase,
                                                                    Publication publication, Username curator)
        throws ApiGatewayException {
        publishPublicationAndFiles(publication);
        return createAutoApprovedTicketForCurator(publishingRequestCase, publication, curator);
    }

    private PublishingRequestCase createPublishingRequestForNonCurator(PublishingRequestCase publishingRequestCase,
                                                                       Publication publication, Username curator)
        throws ApiGatewayException {
        if (REGISTRATOR_PUBLISHES_METADATA_AND_FILES.equals(publishingRequestCase.getWorkflow())) {
            publishPublicationAndFiles(publication);
            return createAutoApprovedTicket(publishingRequestCase, publication, curator);
        }
        if (REGISTRATOR_PUBLISHES_METADATA_ONLY.equals(publishingRequestCase.getWorkflow())) {
            publishMetadata(publication);
            return createAutoApprovedTicketWhenPublicationContainsMetadataOnly(publishingRequestCase, publication,
                                                                               curator);
        } else {
            return (PublishingRequestCase) publishingRequestCase.persistNewTicket(ticketService);
        }
    }

    private PublishingRequestCase createAutoApprovedTicketForCurator(PublishingRequestCase publishingRequestCase,
                                                                     Publication publication, Username curator)
        throws ApiGatewayException {
        publishingRequestCase.setAssignee(curator);
        publishingRequestCase.emptyFilesForApproval();
        return publishingRequestCase.persistAutoComplete(ticketService, publication, curator);
    }

    private PublishingRequestCase createAutoApprovedTicketWhenPublicationContainsMetadataOnly(
        PublishingRequestCase ticket, Publication publication, Username finalizedBy) throws ApiGatewayException {
        if (hasNoFiles(publication)) {
            return createAutoApprovedTicket(ticket, publication, finalizedBy);
        } else {
            return (PublishingRequestCase) ticket.persistNewTicket(ticketService);
        }
    }

    private PublishingRequestCase createAutoApprovedTicket(PublishingRequestCase ticket, Publication publication,
                                                           Username finalizedBy) throws ApiGatewayException {
        ticket.emptyFilesForApproval();
        return ticket.persistAutoComplete(ticketService, publication, finalizedBy);
    }

    private PublishingRequestCase updatePublishingRequestWorkflow(PublishingRequestCase ticket, URI customerId)
        throws BadGatewayException {
        var customerTransactionResult = getCustomerPublishingWorkflowResponse(customerId);
        ticket.setWorkflow(customerTransactionResult.convertToPublishingWorkflow());
        return ticket;
    }

    private CustomerPublishingWorkflowResponse getCustomerPublishingWorkflowResponse(URI customerId)
        throws BadGatewayException {
        var response = uriRetriever.getRawContent(customerId, CONTENT_TYPE)
                           .orElseThrow(this::createBadGatewayException);
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response,
                                                                 CustomerPublishingWorkflowResponse.class)).orElseThrow();
    }

    private boolean isPublishingRequestCase(TicketEntry ticket) {
        return ticket instanceof PublishingRequestCase;
    }

    private TicketEntry persistTicket(TicketEntry newTicket) {
        return attempt(() -> newTicket.persistNewTicket(ticketService)).orElseThrow();
    }

    private void publishPublicationAndFiles(Publication publication) throws ApiGatewayException {
        var updatedPublication = toPublicationWithPublishedFiles(publication);
        publishPublication(updatedPublication);
    }

    private void publishPublication(Publication publication) throws ApiGatewayException {
        resourceService.updatePublication(publication);
        resourceService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
    }

    private void publishMetadata(Publication publication) throws ApiGatewayException {
        publishPublication(publication);
    }

    private Publication toPublicationWithPublishedFiles(Publication publication) {
        return publication.copy().withAssociatedArtifacts(convertFilesToPublished(publication)).build();
    }

    private List<AssociatedArtifact> convertFilesToPublished(Publication publication) {
        return publication.getAssociatedArtifacts().stream().map(this::updateFileToPublished).toList();
    }

    private AssociatedArtifact updateFileToPublished(AssociatedArtifact artifact) {
        if (isNotAdministrativeAgreement(artifact)) {
            return ((File) artifact).toPublishedFile();
        } else {
            return artifact;
        }
    }

    private BadGatewayException createBadGatewayException() {
        return new BadGatewayException("Unable to fetch customerId publishing workflow from upstream");
    }
}
