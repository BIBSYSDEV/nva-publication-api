package publication.test;

import java.util.stream.Stream;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
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
import org.junit.jupiter.params.provider.Arguments;

public final class TicketTestUtils {

    public static Stream<Arguments> ticketTypeAndPublicationStatusProvider() {
        return Stream.of(Arguments.of(DoiRequest.class, PublicationStatus.PUBLISHED),
                         Arguments.of(PublishingRequestCase.class, PublicationStatus.DRAFT),
                         Arguments.of(GeneralSupportRequest.class, PublicationStatus.DRAFT));
    }

    public static Publication createPublication(PublicationStatus status, ResourceService resourceService)
        throws ApiGatewayException {
        var publication = randomPublicationWithStatus(status);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService,
                                                                 UserInstance.fromPublication(publication));
        if(PublicationStatus.PUBLISHED.equals(status)) {
            resourceService.publishPublication(UserInstance.fromPublication(persistedPublication),
                                                             persistedPublication.getIdentifier());
        }
        return resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
    }

    private static Publication randomPublicationWithStatus(PublicationStatus status) {
        return PublicationGenerator.randomPublication().copy()
                   .withDoi(null)
                   .withStatus(status)
                   .build();
    }

    public static TicketEntry createTicket(Publication publication, Class<? extends TicketEntry> ticketType,
                                     TicketService ticketService)
        throws ApiGatewayException {
        return TicketEntry.requestNewTicket(publication, ticketType).persistNewTicket(ticketService);
    }
}
