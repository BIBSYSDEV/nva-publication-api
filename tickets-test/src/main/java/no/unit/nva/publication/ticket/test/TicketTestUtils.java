package no.unit.nva.publication.ticket.test;

import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.model.testing.PublicationGenerator.randomDoi;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.AdministrativeAgreement;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.License;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import org.junit.jupiter.params.provider.Arguments;

public final class TicketTestUtils {

    private static final Set<PublicationStatus> PUBLISHED_STATUSES = Set.of(PUBLISHED,
                                                                            PUBLISHED_METADATA);

    public static Stream<Arguments> ticketTypeAndPublicationStatusProvider() {
        return Stream.of(Arguments.of(DoiRequest.class, PUBLISHED),
                         Arguments.of(DoiRequest.class, PUBLISHED_METADATA),
                         Arguments.of(PublishingRequestCase.class, DRAFT),
                         Arguments.of(GeneralSupportRequest.class, DRAFT));
    }

    public static Publication createNonPersistedPublication(PublicationStatus status) {
        return randomPublicationWithStatus(status);
    }

    public static Publication createPersistedPublication(PublicationStatus status, ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomPublicationWithStatus(status);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService,
                                                                                    UserInstance.fromPublication(
                                                                                        publication));
        if (isPublished(publication)) {
            publishPublication(resourceService, persistedPublication);
            return resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
        }
        return persistedPublication;
    }

    public static Publication createPersistedPublicationWithAdministrativeAgreement(PublicationStatus status,
                                                                             ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of(administrativeAgreement())).build();
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService,
                                                                                    UserInstance.fromPublication(
                                                                                        publication));
        if (isPublished(publication)) {
            publishPublication(resourceService, persistedPublication);
            return resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
        }
        return persistedPublication;
    }

    public static Publication createPersistedPublicationWithUnpublishedFiles(PublicationStatus status,
                                                                             ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomPublicationWithUnpublishedFiles(status);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService,
                                                                                    UserInstance.fromPublication(
                                                                                        publication));
        if (isPublished(publication)) {
            publishPublication(resourceService, persistedPublication);
            return resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
        }
        return persistedPublication;
    }

    public static Publication createPersistedPublicationWithDoi(PublicationStatus status,
                                                                ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomPublicationWithStatusAndDoi(status);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService,
                                                                                    UserInstance.fromPublication(
                                                                                        publication));
        if (PUBLISHED.equals(status)) {
            publishPublication(resourceService, persistedPublication);
            return resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
        }
        return persistedPublication;
    }

    public static Publication createPersistedPublicationWithOwner(PublicationStatus status,
                                                                  UserInstance owner,
                                                                  ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomPublicationWithStatusAndOwner(status, owner);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, owner);
        if (PUBLISHED.equals(status) || PUBLISHED_METADATA.equals(status)) {
            publishPublication(resourceService, persistedPublication);
            return resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
        }
        return persistedPublication;
    }

    public static TicketEntry createPersistedTicket(Publication publication, Class<? extends TicketEntry> ticketType,
                                                    TicketService ticketService)
        throws ApiGatewayException {
        return TicketEntry.requestNewTicket(publication, ticketType).persistNewTicket(ticketService);
    }

    public static TicketEntry createClosedTicket(Publication publication, Class<? extends TicketEntry> ticketType,
                                                 TicketService ticketService)
        throws ApiGatewayException {
        return TicketEntry.createNewTicket(publication, ticketType, SortableIdentifier::next)
                   .persistNewTicket(ticketService).close();
    }

    public static TicketEntry createCompletedTicket(Publication publication, Class<? extends TicketEntry> ticketType,
                                                    TicketService ticketService)
        throws ApiGatewayException {
        var ticket = TicketEntry.createNewTicket(publication, ticketType, SortableIdentifier::next)
                         .persistNewTicket(ticketService);
        ticketService.updateTicketStatus(ticket, TicketStatus.COMPLETED);

        return ticketService.fetchTicket(ticket);
    }

    public static TicketEntry createNonPersistedTicket(Publication publication, Class<? extends TicketEntry> ticketType)
        throws ConflictException {
        return TicketEntry.createNewTicket(publication, ticketType, SortableIdentifier::next);
    }

    private static boolean isPublished(Publication publication) {
        return PUBLISHED_STATUSES.contains(publication.getStatus());
    }

    private static void publishPublication(ResourceService resourceService, Publication persistedPublication)
        throws ApiGatewayException {
        resourceService.publishPublication(UserInstance.fromPublication(persistedPublication),
                                           persistedPublication.getIdentifier());
    }

    private static Publication randomPublicationWithStatusAndOwner(PublicationStatus status, UserInstance owner) {
        return randomPublicationWithStatus(status).copy()
                   .withResourceOwner(new ResourceOwner(new Username(owner.getUsername()), null))
                   .build();
    }

    private static Publication randomPublicationWithStatus(PublicationStatus status) {
        return PublicationGenerator.randomPublication().copy()
                   .withDoi(null)
                   .withStatus(status)
                   .build();
    }

    private static Publication randomPublicationWithUnpublishedFiles(PublicationStatus status) {
        var publication = randomPublication().copy()
                              .withStatus(status)
                              .build();
        unpublishFiles(publication);
        publication.getEntityDescription().setApprovedBy(null);
        return publication;
    }

    private static Publication unpublishFiles(Publication publication) {
        var list = publication.getAssociatedArtifacts()
                       .stream()
                       .filter(artifact -> artifact instanceof File)
                       .map(File.class::cast)
                       .map(File::toUnpublishedFile)
                       .collect(Collectors.toCollection(() -> new ArrayList<AssociatedArtifact>()));
        publication.setAssociatedArtifacts(new AssociatedArtifactList(list));

        return publication;
    }

    private static Publication randomPublicationWithStatusAndDoi(PublicationStatus status) {
        return PublicationGenerator.randomPublication().copy()
                   .withDoi(randomDoi())
                   .withStatus(status)
                   .build();
    }

    private static AdministrativeAgreement administrativeAgreement() {
        var license = new License.Builder().withLink(randomUri()).withIdentifier("identifier").build();
        return new AdministrativeAgreement(UUID.randomUUID(), "name", "application/json",
                                           123124124L, license, true, false, null);
    }
}
