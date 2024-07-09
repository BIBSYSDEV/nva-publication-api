package no.unit.nva.model;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.model.role.Role;
import org.junit.jupiter.api.Test;

public class RoleTest {

    @Test
    void shouldThrowExceptionWhenParsingUnknownRole() {
        assertThrows(IllegalArgumentException.class, () -> Role.parse(randomString()));
    }
}
