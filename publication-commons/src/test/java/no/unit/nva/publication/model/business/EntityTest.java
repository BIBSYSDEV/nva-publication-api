package no.unit.nva.publication.model.business;

import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.time.Clock;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import no.unit.nva.publication.ticket.test.TicketTestUtils;

class EntityTest extends ResourcesLocalTest {
    
    public static final ResourceService SHOULD_NOT_USE_RESOURCE_SERVICE = null;
    private ResourceService resourceService;
    
    @BeforeEach
    public void setup() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
    }
    
    @ParameterizedTest
    @DisplayName("should return referenced stored publication when entity is referencing a publication ")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnReferencedStoredPublicationWhenEntityIsReferencingAPublication(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createNonPersistedTicket(publication, ticketType);
        var storedPublication = ticket.toPublication(resourceService);
        assertThat(storedPublication, is(equalTo(publication)));
    }
    
    @Test
    void shouldReturnEquivalentPublicationWhenEntityIsInternalRepresentationOfPublication() {
        var publication = createDraftPublicationWithoutDoi();
        var resource = Resource.fromPublication(publication);
        var regeneratedPublication = resource.toPublication(SHOULD_NOT_USE_RESOURCE_SERVICE);
        assertThat(regeneratedPublication, is(equalTo(publication)));
    }
    
    private Publication createDraftPublicationWithoutDoi() {
        var publication = randomPublication().copy().withDoi(null).withStatus(DRAFT).build();
        return Resource.fromPublication(publication).persistNew(resourceService,
            UserInstance.fromPublication(publication));
    }
}