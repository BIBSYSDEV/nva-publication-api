package no.unit.nva.model.associatedartifacts.file;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

public class UploadDetailsTest {

    @Test
    public void shouldDeserializeImportUploadDetailsCorrectly() throws JsonProcessingException {
        var json = """
              {
              "type" : "ImportUploadDetails",
                  "system" : "Brage",
                  "archive" : "oda",
                  "uploadedDate" : "2024-07-12T20:15:27.968259325Z"
                }
            """;
        var uploadDetails = dtoObjectMapper.readValue(json, UploadDetails.class);

        assertInstanceOf(ImportUploadDetails.class, uploadDetails);
    }

    @Test
    public void shouldDeserializeUserUploadDetailsCorrectly() throws JsonProcessingException {
        var json = """
              {
              "type" : "UserUploadDetails",
                  "uploadedBy" : "12345@12345",
                  "uploadedDate" : "2024-07-12T20:15:27.968259325Z"
                }
            """;
        var uploadDetails = dtoObjectMapper.readValue(json, UploadDetails.class);

        assertInstanceOf(UserUploadDetails.class, uploadDetails);
    }
}