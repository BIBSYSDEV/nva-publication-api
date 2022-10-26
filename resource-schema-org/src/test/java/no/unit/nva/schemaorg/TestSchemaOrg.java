package no.unit.nva.schemaorg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


class TestSchemaOrg {
    @Test
    void shouldExist() {
        assertDoesNotThrow(SchemaOrg::new);
    }
}
