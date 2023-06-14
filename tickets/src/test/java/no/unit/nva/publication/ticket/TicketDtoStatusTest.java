package no.unit.nva.publication.ticket;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class TicketDtoStatusTest {

    public static final String UNKNOWN_VALUE = "ObviouslyUnknownValue";

    @Test
    void shouldThrowExceptionWhenInputIsUnknownValue() {
        Executable executable = () -> TicketDtoStatus.parse(UNKNOWN_VALUE);
        assertThrows(IllegalArgumentException.class, executable);
    }
}