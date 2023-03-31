package no.unit.nva.publication.ticket.test;

import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.model.testing.PublicationGenerator.randomDoi;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import org.junit.jupiter.params.provider.Arguments;

public final class TicketTestUtils {

    private static final Set<PublicationStatus> validPublishedPublicationStatus = Set.of(PUBLISHED,
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
        if (validPublishedPublicationStatus.contains(publication.getStatus())) {
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

    public static TicketEntry createNonPersistedTicket(Publication publication, Class<? extends TicketEntry> ticketType)
        throws ConflictException {
        return TicketEntry.createNewTicket(publication, ticketType, SortableIdentifier::next);
    }

    private static void publishPublication(ResourceService resourceService, Publication persistedPublication)
        throws ApiGatewayException {
        resourceService.publishPublication(UserInstance.fromPublication(persistedPublication),
                                           persistedPublication.getIdentifier());
    }

    private static Publication randomPublicationWithStatusAndOwner(PublicationStatus status, UserInstance owner) {
        return randomPublicationWithStatus(status).copy()
                   .withResourceOwner(new ResourceOwner(owner.getUsername(), null))
                   .build();
    }

    private static Publication randomPublicationWithStatus(PublicationStatus status) {
        return PublicationGenerator.randomPublication().copy()
                   .withDoi(null)
                   .withStatus(status)
                   .build();
    }

    private static Publication randomPublicationWithStatusAndDoi(PublicationStatus status) {
        return PublicationGenerator.randomPublication().copy()
                   .withDoi(randomDoi())
                   .withStatus(status)
                   .build();
    }
}
