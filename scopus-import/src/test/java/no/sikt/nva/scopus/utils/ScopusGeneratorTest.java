package no.sikt.nva.scopus.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;

class ScopusGeneratorTest {

    @Test
    void shouldReturnDocumentWithAllKnownFieldsNonEmpty() {
        var scopusGenerator = new ScopusGenerator();
        assertDoesNotThrow(scopusGenerator::randomDocument);
    }
}
