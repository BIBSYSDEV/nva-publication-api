package no.unit.nva.model.associatedartifacts.file;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.ImportUploadDetails.Source;
import org.junit.Test;

public class UploadDetailsTest {

    @Test
    public void deserializedImportUploadDetailShouldHaveTypeProperty() throws JsonProcessingException {
        var json = """
                        {
              "type": "Publication",
              "associatedArtifacts": [
                {
                  "type": "PublishedFile",
                  "identifier": "d576be6d-de4d-42b8-aa5a-bf8f820ac36c",
                  "name": "imported_file",
                  "size": 665414,
                  "uploadDetails": {
                    "type": "ImportUploadDetails",
                    "system": "Brage",
                    "archive": "oda",
                    "uploadedDate": "2024-07-12T20:15:27.968259325Z"
                  }
                }
              ]
            }""";
        var publication = dtoObjectMapper.readValue(json, Publication.class);
        var roundTrippedJson = dtoObjectMapper.writeValueAsString(publication);
        var roundTrippedPublication = dtoObjectMapper.readTree(roundTrippedJson);

        var associatedArtifacts = roundTrippedPublication.get("associatedArtifacts");

        for (JsonNode artifact : associatedArtifacts) {
            JsonNode uploadDetails = artifact.get("uploadDetails");
            assertThat(uploadDetails.get("type").asText(), is(equalTo(ImportUploadDetails.TYPE)));        }
    }

    @Test
    public void deserializedUserUploadDetailShouldHaveTypeProperty() throws JsonProcessingException {
        var json = """
                        {
              "type": "Publication",
              "associatedArtifacts": [
                {
                  "type": "UnpublishableFile",
                  "identifier": "206de8de-a628-4d31-adf3-b83dfffc06f1",
                  "name": "user_file",
                  "uploadDetails": {
                    "type": "UserUploadDetails",
                    "uploadedBy": "1234@215.0.0.0",
                    "uploadedDate": "2024-07-12T20:15:27.968310156Z"
                  }
                }
              ]
            }""";
        var publication = dtoObjectMapper.readValue(json, Publication.class);
        var roundTrippedJson = dtoObjectMapper.writeValueAsString(publication);
        var roundTrippedPublication = dtoObjectMapper.readTree(roundTrippedJson);

        var associatedArtifacts = roundTrippedPublication.get("associatedArtifacts");

        for (JsonNode artifact : associatedArtifacts) {
            JsonNode uploadDetails = artifact.get("uploadDetails");
            assertThat(uploadDetails.get("type").asText(), is(equalTo(UserUploadDetails.TYPE)));
        }
    }

    @Test
    public void shouldMigrateUploadDetailsToImportUploadDetailsWhenUploadedByContainsStringInsteadOfUsername()
        throws JsonProcessingException {
        var uploadDetailsThatHaveBeenImported = """
             {
                  "type" : "UploadDetails",
                  "uploadedBy" : "ntnu@194.0.0.0",
                  "uploadedDate" : "2024-07-02T17:10:13.960576174Z"
                }
            """;
        var importDetails = (ImportUploadDetails) dtoObjectMapper.readValue(uploadDetailsThatHaveBeenImported,
                                                                           UploadDetails.class);
        assertEquals("ntnu", importDetails.archive());
        assertEquals(Source.BRAGE, importDetails.source());
    }

    @Test
    public void shouldMigrateUploadDetailsToUserUploadDetailsWhenUploadedByContainsIntegerAsUsername()
        throws JsonProcessingException {
        var uploadDetailsThatHaveBeenImported = """
             {
                  "type" : "UploadDetails",
                  "uploadedBy" : "123454@194.0.0.0",
                  "uploadedDate" : "2024-07-02T17:10:13.960576174Z"
                }
            """;
        var uploadDetails = (UserUploadDetails) dtoObjectMapper.readValue(uploadDetailsThatHaveBeenImported, UploadDetails.class);

        assertEquals(new Username("123454@194.0.0.0"), uploadDetails.uploadedBy());
    }

    @Test
    public void shouldDeserializeImportUploadDetailsCorrectly() throws JsonProcessingException {
        var json = """
              {
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
                  "uploadedBy" : "12345@12345",
                  "uploadedDate" : "2024-07-12T20:15:27.968259325Z"
                }
            """;
        var uploadDetails = dtoObjectMapper.readValue(json, UploadDetails.class);

        assertInstanceOf(UserUploadDetails.class, uploadDetails);
    }

    @Test
    public void shouldDeserializePublicationWithUploadDetailsCorrectly() throws JsonProcessingException {
        var json = """
                        {
              "type": "Publication",
              "associatedArtifacts": [
                {
                  "type": "UnpublishableFile",
                  "identifier": "206de8de-a628-4d31-adf3-b83dfffc06f1",
                  "name": "user_file",
                  "uploadDetails": {
                    "type": "UploadDetails",
                    "uploadedBy": "1234@215.0.0.0",
                    "uploadedDate": "2024-07-12T20:15:27.968310156Z"
                  }
                },
                {
                  "type": "PublishedFile",
                  "identifier": "d576be6d-de4d-42b8-aa5a-bf8f820ac36c",
                  "name": "imported_file",
                  "size": 665414,
                  "uploadDetails": {
                    "type": "UploadDetails",
                    "uploadedBy": "oda@215.0.0.0",
                    "uploadedDate": "2024-07-12T20:15:27.968259325Z"
                  }
                }
              ]
            }""";
        var publication = dtoObjectMapper.readValue(json, Publication.class);
        var roundTrippedJson = dtoObjectMapper.writeValueAsString(publication);
        var roundTrippedPublication = dtoObjectMapper.readValue(roundTrippedJson, Publication.class);

        var files = roundTrippedPublication.getAssociatedArtifacts();

        var uploadDetailsForUserUploadedFile = files.stream()
                                                   .map(File.class::cast)
                                                   .filter(file -> "user_file".equals(file.getName()))
                                                   .map(File::getUploadDetails)
                                                   .findFirst()
                                                   .orElseThrow();

        var importDetailsForImportUploadedFile = files.stream()
                                                     .map(File.class::cast)
                                                     .filter(file -> "imported_file".equals(file.getName()))
                                                     .map(File::getUploadDetails)
                                                     .findFirst()
                                                     .orElseThrow();

        assertInstanceOf(UserUploadDetails.class, uploadDetailsForUserUploadedFile);
        assertInstanceOf(ImportUploadDetails.class, importDetailsForImportUploadedFile);
    }
}