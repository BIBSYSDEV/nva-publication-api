package no.unit.nva.publication.file.upload.restmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import org.junit.jupiter.api.Test;

class CompleteUploadRequestTest {

    @Test
    void shouldSerializeAndDeserializeExternalCompleteUploadRequest() throws JsonProcessingException {
        var json = """
            {
                "type": "ExternalCompleteUpload",
                "uploadId": "abcde123",
                "key": "8d1dc28f-fec2-452f-9d58-da3d1ff03e87",
                "parts": [
                    {
                        "partNumber": "1",
                        "size": "174907",
                        "etag": "34401a771b3e1dbb0d759cedb2fbf02f"
                    }
                ],
                "fileType": "OpenFile"
            }
            """;

        var externalCompleteUploadRequest = JsonUtils.dtoObjectMapper.readValue(json,
                                                                                ExternalCompleteUploadRequest.class);

        var expected = new ExternalCompleteUploadRequest("abcde123", "8d1dc28f-fec2-452f-9d58-da3d1ff03e87", List.of(
            new CompleteUploadPart(1, "34401a771b3e1dbb0d759cedb2fbf02f")), OpenFile.TYPE, null, null, null);

        assertEquals(expected, externalCompleteUploadRequest);
    }

    @Test
    void shouldSerializeAndDeserializeExternalCompleteUploadRequest2() throws JsonProcessingException {
        var json = """
            {
                "type": "ExternalCompleteUpload",
                "uploadId": "abcde123",
                "key": "8d1dc28f-fec2-452f-9d58-da3d1ff03e87",
                "parts": [
                    {
                        "PartNumber": "1",
                        "size": "174907",
                        "ETag": "34401a771b3e1dbb0d759cedb2fbf02f"
                    }
                ],
                "fileType": "OpenFile"
            }
            """;

        var externalCompleteUploadRequest = JsonUtils.dtoObjectMapper.readValue(json,
                                                                                ExternalCompleteUploadRequest.class);

        var expected = new ExternalCompleteUploadRequest("abcde123", "8d1dc28f-fec2-452f-9d58-da3d1ff03e87", List.of(
            new CompleteUploadPart(1, "34401a771b3e1dbb0d759cedb2fbf02f")), OpenFile.TYPE, null, null, null);

        assertEquals(expected, externalCompleteUploadRequest);
    }

    @Test
    void shouldBePossibleToConsumeTwoEtagProperties() throws JsonProcessingException {
        var json = """
            {
                "type": "ExternalCompleteUpload",
                "uploadId": "abcde123",
                "key": "8d1dc28f-fec2-452f-9d58-da3d1ff03e87",
                "parts": [
                    {
                        "PartNumber": "1",
                        "size": "174907",
                        "ETag": "34401a771b3e1dbb0d759cedb2fbf02f",
                        "etag": "34401a771b3e1dbb0d759cedb2fbf02f"
                    }
                ],
                "fileType": "OpenFile"
            }
            """;

        var externalCompleteUploadRequest = JsonUtils.dtoObjectMapper.readValue(json,
                                                                                CompleteUploadRequest.class);

        var expected = new ExternalCompleteUploadRequest("abcde123", "8d1dc28f-fec2-452f-9d58-da3d1ff03e87", List.of(
            new CompleteUploadPart(1, "34401a771b3e1dbb0d759cedb2fbf02f")), OpenFile.TYPE, null, null, null);

        assertEquals(expected, externalCompleteUploadRequest);
    }
}