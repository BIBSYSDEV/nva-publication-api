package no.unit.nva.publication.ticket.create;

import static no.unit.nva.model.PublicationOperation.DOI_REQUEST_CREATE;
import static no.unit.nva.model.PublicationOperation.PUBLISHING_REQUEST_CREATE;
import static no.unit.nva.model.PublicationOperation.SUPPORT_REQUEST_CREATE;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_AUTH_URL;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_SECRET_NAME;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.permission.strategy.PublicationPermissionStrategy;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.DoiRequestDto;
import no.unit.nva.publication.ticket.GeneralSupportRequestDto;
import no.unit.nva.publication.ticket.PublishingRequestDto;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.model.identityservice.CustomerPublishingWorkflowResponse;
import no.unit.nva.publication.utils.RequestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TicketResolver {

    public static final String CONTENT_TYPE = "application/json";
    public static final String CREATING_TICKET_ERROR_MESSAGE =
        "Creating ticket {} for publication {} is forbidden for user {}";
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
        var permissionStrategy = PublicationPermissionStrategy
                                     .create(publication, requestUtils.toUserInstance());

        validateUserPermissions(permissionStrategy, ticketDto, requestUtils);

        var ticket = TicketEntry.requestNewTicket(publication, ticketDto.ticketType())
                         .withOwnerAffiliation(requestUtils.topLevelCristinOrgId())
                         .withOwner(requestUtils.username());
        if (ticket instanceof PublishingRequestCase publishingRequest) {
            var customerId = requestUtils.customerId();
            publishingRequest.withWorkflow(getWorkflow(customerId))
                .withFilesForApproval(getFilesForApproval(publication));
            return createPublishingRequest(publishingRequest, publication, requestUtils);
        }
        return persistTicket(ticket);
    }

    private PublishingWorkflow getWorkflow(URI customerId) throws BadGatewayException {
        return getCustomerPublishingWorkflowResponse(customerId).convertToPublishingWorkflow();
    }

    private Set<File> getFilesForApproval(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .filter(PendingFile.class::isInstance)
                   .map(File.class::cast)
                   .collect(Collectors.toSet());
    }

    private static boolean userHasPermissionToCreateTicket(PublicationPermissionStrategy permissionStrategy,
                                                           TicketDto ticketDto) {
        var allowedActions = permissionStrategy.getAllAllowedActions();
        return switch (ticketDto) {
            case DoiRequestDto ignored -> allowedActions.contains(DOI_REQUEST_CREATE);
            case PublishingRequestDto ignored -> allowedActions.contains(PUBLISHING_REQUEST_CREATE);
            case GeneralSupportRequestDto ignored -> allowedActions.contains(SUPPORT_REQUEST_CREATE);
            case null, default -> false;
        };
    }

    private static boolean hasNoFiles(Publication publication) {
        return publication.getAssociatedArtifacts().stream().noneMatch(File.class::isInstance);
    }

    private void validateUserPermissions(PublicationPermissionStrategy permissionStrategy, TicketDto ticketDto,
                                         RequestUtils requestUtils)
        throws ForbiddenException, NotFoundException {
        if (!userHasPermissionToCreateTicket(permissionStrategy, ticketDto)) {
            logger.error(CREATING_TICKET_ERROR_MESSAGE,
                         ticketDto.ticketType().getSimpleName(),
                         requestUtils.publicationIdentifier(),
                         requestUtils.username());
            throw new ForbiddenException();
        }
    }

    private Publication fetchPublication(RequestUtils requestUtils) throws ApiGatewayException {
        return attempt(requestUtils::publicationIdentifier).map(resourceService::getPublicationByIdentifier)
                   .orElseThrow(fail -> loggingFailureReporter(fail.getException()));
    }

    private ApiGatewayException loggingFailureReporter(Exception exception) {
        logger.error("Request failed: {}", exception.getMessage());
        return new ForbiddenException();
    }

    private PublishingRequestCase createPublishingRequest(PublishingRequestCase publishingRequestCase,
                                                          Publication publication, RequestUtils requestUtils)
        throws ApiGatewayException {

        var username = new Username(requestUtils.username());

        return switch (publishingRequestCase.getWorkflow()) {
            case REGISTRATOR_PUBLISHES_METADATA_AND_FILES ->
                persistCompletedPublishingRequest(publishingRequestCase, publication, username);
            case REGISTRATOR_PUBLISHES_METADATA_ONLY ->
                persistPublishingRequest(publishingRequestCase, publication, username);
            default -> (PublishingRequestCase) publishingRequestCase.persistNewTicket(ticketService);
        };
    }

    private PublishingRequestCase persistCompletedPublishingRequest(
        PublishingRequestCase publishingRequestCase, Publication publication, Username curator)
        throws ApiGatewayException {
        publishingRequestCase.setAssignee(curator);
        return publishingRequestCase.approveFiles().persistAutoComplete(ticketService, publication, curator);
    }

    private PublishingRequestCase persistPublishingRequest(PublishingRequestCase publishingRequestCase,
                                                           Publication publication, Username username)
        throws ApiGatewayException {
        if (hasNoFiles(publication)) {
            return createAutoApprovedTicket(publishingRequestCase, publication, username);
        } else {
            return (PublishingRequestCase) publishingRequestCase.persistNewTicket(ticketService);
        }
    }

    private PublishingRequestCase createAutoApprovedTicket(PublishingRequestCase ticket, Publication publication,
                                                           Username finalizedBy) throws ApiGatewayException {
        ticket.emptyFilesForApproval();
        return ticket.persistAutoComplete(ticketService, publication, finalizedBy);
    }

    private CustomerPublishingWorkflowResponse getCustomerPublishingWorkflowResponse(URI customerId)
        throws BadGatewayException {
        var response = uriRetriever.getRawContent(customerId, CONTENT_TYPE)
                           .orElseThrow(this::createBadGatewayException);
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response, CustomerPublishingWorkflowResponse.class))
                   .orElseThrow();
    }

    private TicketEntry persistTicket(TicketEntry newTicket) {
        return attempt(() -> newTicket.persistNewTicket(ticketService)).orElseThrow();
    }

    private BadGatewayException createBadGatewayException() {
        return new BadGatewayException("Unable to fetch customerId publishing workflow from upstream");
    }
}
