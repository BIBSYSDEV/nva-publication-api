package no.unit.nva.expansion.model;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class ExpandedTicketStatusTest {

    public static final String UNKNOWN_VALUE = "ObviouslyUnknownValue";

    @Test
    void shouldThrowExceptionWhenInputIsUnknownValue() {
        Executable executable = () -> ExpandedTicketStatus.parse(UNKNOWN_VALUE);
        assertThrows(IllegalArgumentException.class, executable);
    }

}