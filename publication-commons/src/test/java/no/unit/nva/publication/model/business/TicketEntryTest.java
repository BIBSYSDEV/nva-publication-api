package no.unit.nva.publication.model.business;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.util.stream.Stream;
import no.unit.nva.publication.testing.TypeProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TicketEntryTest {
    
    public static Stream<Class<?>> ticketTypeProv() {
        return TypeProvider.listSubTypes(TicketEntry.class);
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should request a new ticket ")
    @MethodSource("ticketTypeProv")
    void shouldRequestNewTicket(Class<? extends TicketEntry> ticketType) {
        var publication = randomPublication();
        var ticket = TicketEntry.requestNewTicket(publication, ticketType);
        var actualUserInstance = UserInstance.fromTicket(ticket);
        var expectedUserInstance =
            UserInstance.create(publication.getResourceOwner().getOwner(), publication.getPublisher().getId());
        
        assertThat(ticket.getClass(), is(equalTo(ticketType)));
        assertThat(ticket.extractPublicationIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(actualUserInstance, is(equalTo(expectedUserInstance)));
    }
}