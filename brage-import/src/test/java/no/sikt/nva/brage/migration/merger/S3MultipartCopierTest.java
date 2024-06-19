package no.sikt.nva.brage.migration.merger;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import no.sikt.nva.brage.migration.merger.S3MultipartCopier.MultipartCopyException;
import no.sikt.nva.brage.migration.testutils.ExtendedFakeS3Client;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

class S3MultipartCopierTest {

    @Test
    void shouldThrowMultipartCopyExceptionWhenMissingRequiredParameters() {
        assertThrows(MultipartCopyException.class,
                     () -> S3MultipartCopier.fromSourceKey(randomString()).copy(new FakeS3Client()));
    }

    @Test
    void shouldAbortMultipartCopyingWhenUnexpectedError() {
        assertThrows(MultipartCopyException.class,
                     () -> S3MultipartCopier.fromSourceKey(randomString())
                               .destinationBucket(randomString())
                               .destinationKey(randomString())
                               .sourceBucket(randomString())
                               .copy(new ExtendedFakeS3ClientThrowingException()));
    }

    @Test
    void shouldCopyS3ObjectSuccessfully() {
        var s3Client = new ExtendedFakeS3Client();

        S3MultipartCopier.fromSourceKey(randomString())
            .destinationBucket(randomString())
            .destinationKey(randomString())
            .sourceBucket(randomString())
            .copy(s3Client);

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
            .copy(s3Client);

        verify(s3Client).copyObject((CopyObjectRequest) any());
    }


    private static final class ExtendedFakeS3ClientThrowingException extends ExtendedFakeS3Client {

        @Override
        public CompleteMultipartUploadResponse completeMultipartUpload(CompleteMultipartUploadRequest request) {
            throw new UnsupportedOperationException();
        }
    }
}