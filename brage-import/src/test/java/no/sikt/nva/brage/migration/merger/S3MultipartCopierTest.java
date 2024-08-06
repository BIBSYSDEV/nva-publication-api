package no.sikt.nva.brage.migration.merger;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import no.sikt.nva.brage.migration.merger.S3MultipartCopier.MultipartCopyException;
import no.sikt.nva.brage.migration.testutils.ExtendedFakeS3Client;
import no.unit.nva.stubs.FakeS3Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

class S3MultipartCopierTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = mock(Context.class);
        when(context.getRemainingTimeInMillis()).thenReturn(100000);
    }

    @Test
    void shouldThrowMultipartCopyExceptionWhenMissingRequiredParameters() {
        assertThrows(MultipartCopyException.class,
                     () -> S3MultipartCopier.fromSourceKey(randomString()).copy(new FakeS3Client(), context));
    }

    @Test
    void shouldAbortMultipartCopyingWhenUnexpectedError() {
        assertThrows(MultipartCopyException.class,
                     () -> S3MultipartCopier.fromSourceKey(randomString())
                               .destinationBucket(randomString())
                               .destinationKey(randomString())
                               .sourceBucket(randomString())
                               .copy(new ExtendedFakeS3ClientThrowingException(), context));
    }

    @Test
    void shouldCopyS3ObjectSuccessfully() {
        var s3Client = new ExtendedFakeS3Client();

        S3MultipartCopier.fromSourceKey(randomString())
            .destinationBucket(randomString())
            .destinationKey(randomString())
            .sourceBucket(randomString())
            .copy(s3Client, context);

        assertEquals(1, s3Client.getMultipartCopiedResults().size());
    }

    @Test
    void shouldCopyS3ObjectUsingSimpleCopyObjectMethodWhenS3ObjectHasSizeZero() {
        var s3Client = mock(S3Client.class);

        when(s3Client.headObject((HeadObjectRequest) any()))
            .thenReturn(HeadObjectResponse.builder().contentLength(0L).build());

        S3MultipartCopier.fromSourceKey(randomString())
            .destinationBucket(randomString())
            .destinationKey(randomString())
            .sourceBucket(randomString())
            .copy(s3Client, context);

        verify(s3Client).copyObject((CopyObjectRequest) any());
    }

    @Test
    void shouldThrowMultipartCopyExceptionWhenTimeoutThresholdHasBeenExceeded() {
        when(context.getRemainingTimeInMillis()).thenReturn(1);
        assertThrows(MultipartCopyException.class,
                     (() -> S3MultipartCopier.fromSourceKey(randomString()).copy(new FakeS3Client(), context)),
                     "Timeout threshold exceeded when copying associated artifacts!");
    }

    private static final class ExtendedFakeS3ClientThrowingException extends ExtendedFakeS3Client {

        @Override
        public CompleteMultipartUploadResponse completeMultipartUpload(CompleteMultipartUploadRequest request) {
            throw new UnsupportedOperationException();
        }
    }
}