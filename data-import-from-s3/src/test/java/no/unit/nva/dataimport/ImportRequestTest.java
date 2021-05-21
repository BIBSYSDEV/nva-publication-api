package no.unit.nva.dataimport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

class ImportRequestTest {

    public static final String BUCKET_NAME = "orestis-export";
    public static final String FOLDER_PATH = "AWSDynamoDB/01617869890675-2abaf414/data/";
    public static final String S3_LOCATION = "s3://" + BUCKET_NAME + "/" + FOLDER_PATH;

    @Test
    public void importRequestAcceptsS3UriAsInput() throws JsonProcessingException {
        ImportRequest request = new ImportRequest(S3_LOCATION);
        String jsonString = JsonUtils.objectMapperNoEmpty.writeValueAsString(request);
        ImportRequest deserializedRequest = JsonUtils.objectMapper.readValue(jsonString, ImportRequest.class);
        assertThat(deserializedRequest, is(equalTo(request)));
    }

    @Test
    public void getBucketReturnsBucketNameOfS3Uri() {
        ImportRequest request = new ImportRequest(S3_LOCATION);
        String bucket = request.extractBucketFromS3Location();
        assertThat(bucket, is(equalTo(BUCKET_NAME)));
    }

    @Test
    public void getFolderPathReturnsPathInsideTheBucketOfS3Uri() {
        ImportRequest request = new ImportRequest(S3_LOCATION);
        String folderPath = request.extractPathFromS3Location();
        assertThat(folderPath, is(equalTo(FOLDER_PATH)));
    }
}