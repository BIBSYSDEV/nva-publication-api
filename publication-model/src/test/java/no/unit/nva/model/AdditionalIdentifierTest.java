package no.unit.nva.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

@Deprecated
class AdditionalIdentifierTest {

    @Test
    void shouldMigrateSourceStringToSourceName() {
        var oldStyle = """
            {
              "type": "AdditionalIdentifier",
              "source": "someString",
              "value": "some value"
            }""";
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(oldStyle, AdditionalIdentifier.class));
    }

}