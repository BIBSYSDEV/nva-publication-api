package no.unit.nva.publication.ticket.create;

import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_AUTH_URL;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_SECRET_NAME;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.model.identityservice.CustomerTransactionResult;
import nva.commons.core.JacocoGenerated;

public class PublishingRequestResolver {

    public static final String CONTENT_TYPE = "application/json";
    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final RawContentRetriever uriRetriever;

    @JacocoGenerated
    public PublishingRequestResolver() {
        this(ResourceService.defaultService(), TicketService.defaultService(),
             new AuthorizedBackendUriRetriever(BACKEND_CLIENT_SECRET_NAME, BACKEND_CLIENT_AUTH_URL));
    }

    public PublishingRequestResolver(ResourceService resourceService, TicketService ticketService,
                                     RawContentRetriever uriRetriever) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.uriRetriever = uriRetriever;
    }

    public void updateTicketAndPublicationIfNeeded(TicketEntry ticket, Publication publication, URI customer) {
        if (customerAllowsPublishing(customer)) {
            updateStatusToApproved(ticket);
            publishPublication(publication);
        }
    }

    private boolean customerAllowsPublishing(URI customerId) {
        var rawContent = uriRetriever.getRawContent(customerId, CONTENT_TYPE);
        return rawContent.isPresent() &&
               new CustomerTransactionResult(rawContent.get(), customerId).isKnownThatCustomerAllowsPublishing();
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
}
