package no.unit.nva.publication.model.business;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TicketEntryTest {

    public static Stream<Class<?>> publishingAndGeneralSupportTicketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class)
                   .filter(type -> type == PublishingRequestCase.class || type == GeneralSupportRequest.class);
    }

    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should request a new ticket ")
    @MethodSource("publishingAndGeneralSupportTicketTypeProvider")
    void shouldRequestNewTicket(Class<? extends TicketEntry> ticketType) throws BadRequestException {
        var publication = randomPublication();
        var ticket = TicketEntry.requestNewTicket(publication, ticketType);
        var actualUserInstance = UserInstance.fromTicket(ticket);
        var expectedUserInstance =
            UserInstance.create(publication.getResourceOwner().getOwner(), publication.getPublisher().getId());
        
        assertThat(ticket.getClass(), is(equalTo(ticketType)));
        assertThat(ticket.extractPublicationIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(actualUserInstance, is(equalTo(expectedUserInstance)));
    }

    @Test
    void shouldReturnBadRequestWhenRequestingDoiRequestTicketForUnpublishedPublication() {
        var publication = randomPublication();
        assertThrows(BadRequestException.class, () -> TicketEntry.requestNewTicket(publication, DoiRequest.class));
    }

    @Test
    void shouldReturnRuntimeExceptionWhenUnrecognizedTicketType() {
        var publication = randomPublication();
        assertThrows(RuntimeException.class, () -> TicketEntry.requestNewTicket(publication, null));
    }

    @Test
    void shouldReturnUnsupportedOperationCreatingQueryObjectForUnsupportedTicketType() {
        var publication = randomPublication();
        Executable action = () -> TicketEntry.createQueryObject(publication.getPublisher().getId(),
                                                                    publication.getIdentifier(), null);
        assertThrows(UnsupportedOperationException.class, action);
    }

    @Test
    void shouldReturnUnsupportedOperationCreatingNewTicketEntryForUnsupportedTicketType() {
        var publication = randomPublication();
        Executable action = () -> TicketEntry.createNewTicket(publication, null, SortableIdentifier::next);
        assertThrows(UnsupportedOperationException.class, action);
    }
}