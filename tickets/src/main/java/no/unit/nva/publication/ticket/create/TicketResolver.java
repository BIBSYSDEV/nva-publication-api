package no.unit.nva.publication.ticket.create;

import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_AUTH_URL;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_SECRET_NAME;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.AdministrativeAgreement;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.model.identityservice.CustomerPublishingWorkflowResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.JacocoGenerated;

public class TicketResolver {

    public static final String CONTENT_TYPE = "application/json";
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

    public TicketEntry resolveAndPersistTicket(TicketEntry ticket,
                                               Publication publication,
                                               URI customerId,
                                               boolean isCurator) throws ApiGatewayException {
        if (isPublishingRequestCase(ticket)) {
            var publishingRequestCase = updatePublishingRequestWorkflow((PublishingRequestCase) ticket, customerId);
            return createPublishingRequest(publishingRequestCase, publication, isCurator);
        }
        return persistTicket(ticket);
    }

    private static boolean isNotAdministrativeAgreement(AssociatedArtifact artifact) {
        return artifact instanceof File && !(artifact instanceof AdministrativeAgreement);
    }

    private static boolean hasNoFiles(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .noneMatch(artifact -> artifact instanceof File);
    }

    private PublishingRequestCase createPublishingRequest(PublishingRequestCase publishingRequestCase,
                                                          Publication publication,
                                                          boolean isCurator)
        throws ApiGatewayException {
        if (REGISTRATOR_PUBLISHES_METADATA_AND_FILES.equals(publishingRequestCase.getWorkflow()) || isCurator) {
            publishPublicationAndFiles(publication);
            return createAutoApprovedTicket(publishingRequestCase);
        }
        if (REGISTRATOR_PUBLISHES_METADATA_ONLY.equals(publishingRequestCase.getWorkflow())) {
            publishMetadata(publication);
            return createAutoApprovedTicketWhenPublicationContainsMetadataOnly(publishingRequestCase, publication);
        }
        return (PublishingRequestCase) publishingRequestCase.persistNewTicket(ticketService);
    }

    private PublishingRequestCase createAutoApprovedTicketWhenPublicationContainsMetadataOnly(TicketEntry ticket,
                                                                                              Publication publication)
        throws ApiGatewayException {
        if (hasNoFiles(publication)) {
            return createAutoApprovedTicket(ticket);
        } else {
            return (PublishingRequestCase) ticket.persistNewTicket(ticketService);
        }
    }

    private PublishingRequestCase createAutoApprovedTicket(TicketEntry ticket) throws ApiGatewayException {
        return ((PublishingRequestCase) ticket).persistAutoComplete(ticketService);
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
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response, CustomerPublishingWorkflowResponse.class))
                   .orElseThrow();
    }

    private boolean isPublishingRequestCase(TicketEntry ticket) {
        return ticket instanceof PublishingRequestCase;
    }

    private TicketEntry persistTicket(TicketEntry newTicket) throws ApiGatewayException {
        return attempt(() -> newTicket.persistNewTicket(ticketService))
                   .orElse(fail -> handleCreationException(fail.getException(), newTicket));
    }

    private TicketEntry updateAlreadyExistingTicket(TicketEntry newTicket) {
        var customerId = newTicket.getCustomerId();
        var resourceIdentifier = newTicket.getResourceIdentifier();
        return ticketService.fetchTicketByResourceIdentifier(customerId, resourceIdentifier, newTicket.getClass())
                   .map(this::updateTicket)
                   .orElseThrow();
    }

    private TicketEntry updateTicket(TicketEntry ticket) {
        ticket.persistUpdate(ticketService);
        return ticket;
    }

    private TicketEntry handleCreationException(Exception exception, TicketEntry newTicket) throws ApiGatewayException {
        if (exception instanceof TransactionFailedException) {
            return updateAlreadyExistingTicket(newTicket);
        }
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        if (exception instanceof ApiGatewayException) {
            throw (ApiGatewayException) exception;
        }
        throw new RuntimeException(exception);
    }

    private void publishPublicationAndFiles(Publication publication) {
        var updatedPublication = toPublicationWithPublishedFiles(publication);
        attempt(() -> resourceService.updatePublication(updatedPublication));
        publishPublication(updatedPublication);
    }

    private void publishPublication(Publication publication) {
        attempt(() -> resourceService.updatePublication(publication));
        attempt(() -> resourceService.publishPublication(UserInstance.fromPublication(publication),
                                                         publication.getIdentifier()));
    }

    private void publishMetadata(Publication publication) {
        publishPublication(publication);
    }

    private Publication toPublicationWithPublishedFiles(Publication publication) {
        return publication.copy()
                   .withAssociatedArtifacts(convertFilesToPublished(publication))
                   .build();
    }

    private List<AssociatedArtifact> convertFilesToPublished(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .map(this::updateFileToPublished)
                   .collect(Collectors.toList());
    }

    private AssociatedArtifact updateFileToPublished(AssociatedArtifact artifact) {
        if (isNotAdministrativeAgreement(artifact)) {
            return ((File) artifact).toPublishedFile();
        } else {
            return artifact;
        }
    }

    private BadGatewayException createBadGatewayException() {
        return new BadGatewayException("Unable to fetch customer publishing workflow from upstream");
    }
}
