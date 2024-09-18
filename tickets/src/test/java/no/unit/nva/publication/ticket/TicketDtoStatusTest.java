package no.unit.nva.publication.ticket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Arrays;
import no.unit.nva.publication.model.business.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class TicketDtoStatusTest {

    public static final String UNKNOWN_VALUE = "ObviouslyUnknownValue";

    @Test
    void shouldThrowExceptionWhenInputIsUnknownValue() {
        Executable executable = () -> TicketDtoStatus.parse(UNKNOWN_VALUE);
        assertThrows(IllegalArgumentException.class, executable);
    }

    @Test
    void shouldParseAllValidTicketStatuses() {
        assertDoesNotThrow(() -> Arrays.stream(TicketStatus.values())
            .map(TicketStatus::toString)
            .map(TicketDtoStatus::parse).toList());
    }
}