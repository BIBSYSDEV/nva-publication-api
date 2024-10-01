package no.unit.nva.publication.model.business;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import nva.commons.apigateway.exceptions.ConflictException;
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
        var ticket = TicketEntry.requestNewTicket(publication, ticketType, publication.getPublisher().getId());
        var actualUserInstance = UserInstance.fromTicket(ticket);
        var expectedUserInstance = getExpectedUserInstance(publication);

        assertThat(ticket.getClass(), is(equalTo(ticketType)));
        assertThat(ticket.getResourceIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(actualUserInstance, is(equalTo(expectedUserInstance)));
    }

    @Test
    void shouldThrowExceptionForUnrecognizedTicketType() {
        var publication = TicketTestUtils.createNonPersistedPublication(PublicationStatus.DRAFT);
        assertThrows(RuntimeException.class, () -> TicketEntry.requestNewTicket(publication, DoiRequest.class,
                                                                                publication.getPublisher().getId()));
    }

    @Test
    void shouldThrowExceptionForUnrecognizedTicketTypeRequestingNewTicket() {
        var publication = TicketTestUtils.createNonPersistedPublication(PublicationStatus.DRAFT);
        assertThrows(RuntimeException.class, () -> TicketEntry.requestNewTicket(publication, TicketEntry.class,
                                                                                publication.getPublisher().getId()));
    }

    @Test
    void shouldThrowExceptionForUnrecognizedTicketTypeCreatingQueryObject() {
        var publication = TicketTestUtils.createNonPersistedPublication(PublicationStatus.DRAFT);
        assertThrows(UnsupportedOperationException.class,
                     () -> TicketEntry.createQueryObject(publication.getPublisher().getId(),
                                                         publication.getIdentifier(),
                                                         TicketEntry.class));
    }

    @Test
    void shouldThrowExceptionForUnrecognizedTicketCreatingNewTicket() {
        var publication = TicketTestUtils.createNonPersistedPublication(PublicationStatus.DRAFT);
        assertThrows(UnsupportedOperationException.class,
                     () -> TicketEntry.createNewTicket(publication, TicketEntry.class,
                                                       SortableIdentifier::next, publication.getPublisher().getId()));
    }

    @Test
    void shouldReturnFalseWhenTicketWithoutAssignee() throws ConflictException {
        var publication = TicketTestUtils.createNonPersistedPublication(PublicationStatus.DRAFT);
        var ticket = TicketEntry.createNewTicket(publication, DoiRequest.class, SortableIdentifier::next,
                                                 publication.getPublisher().getId());

        assertFalse(ticket.hasAssignee());
    }

    private static UserInstance getExpectedUserInstance(Publication publication) {
        return UserInstance.create(
            publication.getResourceOwner().getOwner().getValue(), publication.getPublisher().getId());
    }
}