package no.unit.nva.publication.model.business;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TicketEntryTest {

    @ParameterizedTest
    @DisplayName("should request a new ticket ")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldRequestNewTicket(Class<? extends TicketEntry> ticketType, PublicationStatus status) {
        var publication = TicketTestUtils.createNonPersistedPublication(status);
        var ticket = TicketEntry.requestNewTicket(publication, ticketType);
        var actualUserInstance = UserInstance.fromTicket(ticket);
        var expectedUserInstance =
            UserInstance.create(publication.getResourceOwner().getOwner().getValue(), publication.getPublisher().getId());

        assertThat(ticket.getClass(), is(equalTo(ticketType)));
        assertThat(ticket.extractPublicationIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(actualUserInstance, is(equalTo(expectedUserInstance)));
    }

    @Test
    void shouldThrowExceptionForUnrecognizedTicketType() {
        var publication = TicketTestUtils.createNonPersistedPublication(PublicationStatus.DRAFT);
        assertThrows(RuntimeException.class, () -> TicketEntry.requestNewTicket(publication, DoiRequest.class));
    }
}