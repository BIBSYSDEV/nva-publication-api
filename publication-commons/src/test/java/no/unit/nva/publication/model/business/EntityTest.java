package no.unit.nva.publication.model.business;

import static no.unit.nva.model.PublicationStatus.DRAFT;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.time.Clock;
import java.util.stream.Stream;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.testing.TypeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EntityTest extends ResourcesLocalTest {
    
    public static final ResourceService SHOULD_NOT_USE_RESOURCE_SERVICE = null;
    private ResourceService resourceService;
    
    public static Stream<Arguments> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class).map(Arguments::of);
    }
    
    @BeforeEach
    public void setup() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
    }
    
    @ParameterizedTest(name = "entity type:{0}")
    @DisplayName("should return referenced stored publication when entity is referencing a publication ")
    @MethodSource("ticketTypeProvider")
    void shouldReturnReferencedStoredPublicationWhenEntityIsReferencingAPublication(
        Class<? extends TicketEntry> ticketType) {
        var publication = createDraftPublicationWithoutDoi();
        var ticket = TicketEntry.requestNewTicket(publication, ticketType);
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
        var publication = randomPreFilledPublicationBuilder().withDoi(null).withStatus(DRAFT).build();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
}