package no.unit.nva;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Organization;
import org.junit.jupiter.api.Test;

public class OrganizationTest {

    @Deprecated
    @Test
    void shouldSerializeOrganizationWithLabelsToOrganizationWithoutLabels() {
        var value = "{\n"
                    + "        \"type\" : \"Organization\",\n"
                    + "        \"id\" : \"https://www.example.org/4a49816e-abc0-42c4-a293-fa32740c2eba\",\n"
                    + "        \"labels\" : {"
                    + "         \"en\" : \"some label\""
                    + "         }   "
                    + "      };";

        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(value, Organization.class));

    }
}
