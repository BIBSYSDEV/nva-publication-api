package no.unit.nva.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

class CuratingInstitutionTest {

    @Test
    void shouldMigrateCuratingInstitution() {
        var json = """
                        {
              "type": "Publication",
              "identifier": "0192995c7fa5-726a014a-7e02-4cf9-82e7-4cfe99e9ac2a",
              "status": "PUBLISHED",
              "curatingInstitutions": [
                "https://some.uri/curatingInstitution/1"
              ]
            }
            """;
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(json, Publication.class));
    }

    @Test
    void shouldSerializeCuratingInstitution() {
        var json = """
                                    {
              "type": "Publication",
              "identifier": "0192995c7fa5-726a014a-7e02-4cf9-82e7-4cfe99e9ac2a",
              "status": "PUBLISHED",
              "curatingInstitutions": [
                {
                  "id": "https://some.uri/curatingInstitution/1",
                  "contributorCristinIds": [
                    "https://some.uri/contributor/1"
                  ]
                }
              ]
            }
            """;
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(json, Publication.class));
    }
}