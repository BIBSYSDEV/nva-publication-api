package no.unit.nva.publication.migration;

import static nva.commons.core.JsonUtils.objectMapperNoEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

class DataMigrationRequestTest {

    public static final String BUCKET_NAME = "orestis-export";
    public static final String FOLDER_PATH = "AWSDynamoDB/01617869890675-2abaf414/data/";
    public static final String S3_LOCATION = "s3://" + BUCKET_NAME + "/" + FOLDER_PATH;

    @Test
    public void importRequestAcceptsS3UriAsInput() throws JsonProcessingException {
        DataMigrationRequest request = new DataMigrationRequest(S3_LOCATION);
        String jsonString = objectMapperNoEmpty.writeValueAsString(request);
        DataMigrationRequest deserializedRequest =
            objectMapperNoEmpty.readValue(jsonString, DataMigrationRequest.class);
        assertThat(deserializedRequest, is(equalTo(request)));
    }

    @Test
    public void getBucketReturnsBucketNameOfS3Uri() {
        DataMigrationRequest request = new DataMigrationRequest(S3_LOCATION);
        String bucket = request.extractBucketFromS3Location();
        assertThat(bucket, is(equalTo(BUCKET_NAME)));
    }

    @Test
    public void getFolderPathReturnsPathInsideTheBucketOfS3Uri() {
        DataMigrationRequest request = new DataMigrationRequest(S3_LOCATION);
        String folderPath = request.extractPathFromS3Location();
        assertThat(folderPath, is(equalTo(FOLDER_PATH)));
    }
}