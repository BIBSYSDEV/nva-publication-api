package no.unit.nva.model;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class ContributorVerificationStatusTest {

    @Test
    void shouldThrowExceptionWhenCannotParseStatus() {
        assertThrows(IllegalArgumentException.class, () -> ContributorVerificationStatus.parse(randomString()));
    }
}
