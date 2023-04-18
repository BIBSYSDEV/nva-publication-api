package no.unit.nva.publication.ticket.create;

import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_AUTH_URL;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_SECRET_NAME;
import static nva.commons.core.attempt.Try.attempt;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
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

    public TicketEntry resolveAndPersistTicket(TicketEntry ticket, Publication publication, URI customerId) throws ApiGatewayException {
        if (isPublishingRequestCase(ticket)) {
            var customerTransactionResult = getCustomerPublishingWorkflowResponse(customerId);
            var publishingWorkflow = customerTransactionResult.convertToPublishingWorkflow();

            if (publishingWorkflow == REGISTRATOR_PUBLISHES_METADATA_AND_FILES) {
                ((PublishingRequestCase) ticket).setWorkflow(publishingWorkflow);
                var persistedTicket = persistTicket(ticket);
                approveTicketAndPublishPublication(persistedTicket, publication);
                return persistedTicket;
            }else if (publishingWorkflow == REGISTRATOR_PUBLISHES_METADATA_ONLY) {
                ((PublishingRequestCase) ticket).setWorkflow(publishingWorkflow);
            }else if(publishingWorkflow == REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES) {
                ((PublishingRequestCase) ticket).setWorkflow(publishingWorkflow);
            }
        }
        return persistTicket(ticket);
    }

    private CustomerPublishingWorkflowResponse getCustomerPublishingWorkflowResponse(URI customerId) throws BadGatewayException {
        var response = uriRetriever.getRawContent(customerId, CONTENT_TYPE).orElseThrow(this::createBadGatewayException);
        return attempt(()-> JsonUtils.dtoObjectMapper.readValue(response, CustomerPublishingWorkflowResponse.class)).orElseThrow();
    }

    private boolean isPublishingRequestCase(TicketEntry ticket) {
        return ticket instanceof PublishingRequestCase;
    }

    private void approveTicketAndPublishPublication(TicketEntry ticket, Publication publication) {
        updateStatusToApproved(ticket);
        publishPublication(publication);
    }

    private TicketEntry persistTicket(TicketEntry newTicket) throws ApiGatewayException {
        return attempt(() -> newTicket.persistNewTicket(ticketService))
                .orElse(fail -> handleCreationException(fail.getException(), newTicket));
    }

    private TicketEntry updateAlreadyExistingTicket(TicketEntry newTicket) {
        var customerId = newTicket.getCustomerId();
        var resourceIdentifier = newTicket.extractPublicationIdentifier();
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

    private void publishPublication(Publication publication) {
        var updatedPublication = toPublicationWithPublishedFiles(publication);
        attempt(() -> resourceService.updatePublication(updatedPublication));
        attempt(() -> resourceService.publishPublication(UserInstance.fromPublication(updatedPublication),
                                                         updatedPublication.getIdentifier()));
    }

    private Publication toPublicationWithPublishedFiles(Publication publication) {
        return publication.copy()
                   .withAssociatedArtifacts(convertFilesToPublished(publication.getAssociatedArtifacts()))
                   .build();
    }

    private List<AssociatedArtifact> convertFilesToPublished(AssociatedArtifactList associatedArtifacts) {
        return associatedArtifacts.stream()
                   .map(this::updateFileToPublished)
                   .collect(Collectors.toList());
    }

    private AssociatedArtifact updateFileToPublished(AssociatedArtifact artifact) {
        if (artifact instanceof File) {
            var file = (File) artifact;
            return file.toPublishedFile();
        } else {
            return artifact;
        }
    }

    private void updateStatusToApproved(TicketEntry createdTicket) {
        attempt(() -> ticketService.updateTicketStatus(createdTicket, TicketStatus.COMPLETED))
            .orElseThrow();
    }

    private BadGatewayException createBadGatewayException() {
        return new BadGatewayException("Unable to fetch customer publishing workflow from upstream");
    }
}
