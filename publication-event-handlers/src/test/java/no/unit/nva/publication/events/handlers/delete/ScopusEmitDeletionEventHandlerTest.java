package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.RequestParametersEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.ResponseElementsEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.UserIdentityEntity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class ScopusEmitDeletionEventHandlerTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final long SOME_FILE_SIZE = 100L;
    public static final InputStream EVENT = IoUtils.inputStreamFromResources(
        "delete/delete_scopus_identifier_list.txt");
    private static final String INPUT_BUCKET_NAME = "some-input-bucket-name";
    private static final RequestParametersEntity EMPTY_REQUEST_PARAMETERS = null;
    private static final ResponseElementsEntity EMPTY_RESPONSE_ELEMENTS = null;
    private static final UserIdentityEntity EMPTY_USER_IDENTITY = null;
    private static final String SCOPUS_ID_FROM_TEST_FILE = "2-s2.0-38349009276";
    private ByteArrayOutputStream outputStream;
    private FakeS3Client s3Client;
    private S3Driver s3Driver;
    private ScopusEmitDeletionEventHandler handler;

    @BeforeEach
    public void init() {
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "ignoredValue");
        this.outputStream = new ByteArrayOutputStream();
        handler = new ScopusEmitDeletionEventHandler(s3Client);
    }

    @Test
    void shouldEmitEventsForEveryScopusIdentifierInDeletionList() throws IOException {
        var logAppender = LogUtils.getTestingAppenderForRootLogger();
        var s3Event = createS3Event(randomString());
        handler.handleRequest(s3Event, CONTEXT);
        assertThat(logAppender.getMessages(), containsString(SCOPUS_ID_FROM_TEST_FILE));
    }

    private S3Event createS3Event(String expectedObjectKey) throws IOException {
        var eventNotification = new S3EventNotificationRecord(randomString(),
                                                              randomString(),
                                                              randomString(),
                                                              randomDate(),
                                                              randomString(),
                                                              EMPTY_REQUEST_PARAMETERS,
                                                              EMPTY_RESPONSE_ELEMENTS,
                                                              createS3Entity(expectedObjectKey),
                                                              EMPTY_USER_IDENTITY);
        s3Driver.insertFile(UnixPath.of(expectedObjectKey), EVENT);
        return new S3Event(List.of(eventNotification));
    }

    private S3Entity createS3Entity(String expectedObjectKey) {
        var bucket = new S3BucketEntity(INPUT_BUCKET_NAME, EMPTY_USER_IDENTITY, randomString());
        var object = new S3ObjectEntity(expectedObjectKey,
                                        SOME_FILE_SIZE,
                                        randomString(),
                                        randomString(),
                                        randomString());
        var schemaVersion = randomString();
        return new S3Entity(randomString(), bucket, object, schemaVersion);
    }

    private String randomDate() {
        return Instant.now().toString();
    }
}
