package no.sikt.nva.scopus;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import java.util.List;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.Test;

class ScopusUnzipperHandlerTest {

    @Test
    void shouldUnzipAndEnqueueFilesFromEntry() {
        var scopusUnzipper = mock(ScopusUnzipper.class);
        var handler = new ScopusUnzipperHandler(scopusUnzipper);

        handler.handleRequest(randomS3Event(), new FakeContext());

        verify(scopusUnzipper).unzipAndEnqueue(any(), any());
    }

    private S3Event randomS3Event() {
        var eventNotification = new S3EventNotificationRecord(randomString(), randomString(), randomString(), null,
                                                              randomString(), null, null, createS3Entity(), null);
        return new S3Event(List.of(eventNotification));
    }

    private S3Entity createS3Entity() {
        var bucket = new S3BucketEntity(randomString(), null, randomString());
        var object = new S3ObjectEntity(randomString(), null, randomString(), randomString(), randomString());
        var schemaVersion = randomString();
        return new S3Entity(randomString(), bucket, object, schemaVersion);
    }
}