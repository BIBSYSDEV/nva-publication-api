package no.unit.nva.model;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PublicationOperationTest {

    @Test
    void shouldLookupEnumWhenStatusIsKnown() {
        assertEquals(PublicationOperation.UPDATE, PublicationOperation.lookup("update"));
    }

    @Test
    void shouldThrowExceptionWhenCannotParseStatus() {
        assertThrows(IllegalArgumentException.class, () -> PublicationOperation.lookup(randomString()));
    }
}