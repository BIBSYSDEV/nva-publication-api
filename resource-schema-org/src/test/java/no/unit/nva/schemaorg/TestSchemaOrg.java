package no.unit.nva.schemaorg;
import no.unit.nva.expansion.model.ExpandedResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


class TestSchemaOrg {
    @Test
    void shouldExist() {
        var expandedResource = new ExpandedResource();
        assertDoesNotThrow(() -> new SchemaOrg(expandedResource));
    }
}
