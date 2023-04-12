package no.unit.nva.publication.model.business;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicationWorkflowTest {

    private static final String INVALID_VALUE = "Werdtf";

    @Test
    void shouldFailWheninvalidEnumSupplied() {

        Executable executable = () -> PublicationWorkflow.lookUp(INVALID_VALUE);
        assertThrows(IllegalArgumentException.class, executable);
    }
}