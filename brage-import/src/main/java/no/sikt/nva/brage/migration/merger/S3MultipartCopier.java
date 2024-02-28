package no.sikt.nva.brage.migration.merger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;

public final class S3MultipartCopier {

    private static final String MULTIPART_UPLOAD_ERROR_MESSAGE = "Could not complete multipart upload: {}";
    private static final String MISSING_REQUIRING_PARAMETERS_MESSAGE = "All required params have to be provided to "
                                                                       + "perform multipart copy!";
    private static final long PARTITION_SIZE = 5L * 1024 * 1024;
    private final Logger logger = LoggerFactory.getLogger(S3MultipartCopier.class);
    private final String sourceKey;
    private final String sourceBucket;
    private final String destinationKey;
    private final String destinationBucket;
    private final List<CompletedPart> completedParts;

    private S3MultipartCopier(String sourceKey, String sourceBucket, String destinationKey, String destinationBucket) {
        this.sourceKey = sourceKey;
        this.sourceBucket = sourceBucket;
        this.destinationKey = destinationKey;
        this.destinationBucket = destinationBucket;
        this.completedParts = new ArrayList<>();
    }

    public static S3MultipartCopier fromSourceKey(String sourceKey) {
        return builder().withSourceKey(sourceKey).build();
    }

    public void copy(S3Client s3Client) {
        validateRequest();
        performCopying(s3Client);
    }

    public S3MultipartCopier sourceBucket(String sourceBucket) {
        return this.copy().withSourceBucket(sourceBucket).build();
    }

    public S3MultipartCopier destinationKey(String destinationKey) {
        return this.copy().withDestinationKey(destinationKey).build();
    }

    public S3MultipartCopier destinationBucket(String destinationBucket) {
        return this.copy().withDestinationBucket(destinationBucket).build();
    }

    private static Builder builder() {
        return new Builder();
    }

    private static String constructSourceRange(long currentPosition, long endPosition) {
        return String.format("bytes=%d-%d", currentPosition, endPosition);
    }

    private static CompletedPart createCompletedPart(int partNumber, UploadPartCopyResponse uploadPartResponse) {
        return CompletedPart.builder().partNumber(partNumber).eTag(uploadPartResponse.copyPartResult().eTag()).build();
    }

    private Builder copy() {
        return builder().withSourceKey(this.sourceKey)
                   .withSourceBucket(this.sourceBucket)
                   .withDestinationKey(this.destinationKey)
                   .withDestinationBucket(this.destinationBucket);
    }

    private CompletedMultipartUpload completeMultipartUpload() {
        return CompletedMultipartUpload.builder().parts(completedParts).build();
    }

    private void validateRequest() {
        if (missingRequiredValues()) {
            throw MultipartCopyException.withMessage(MISSING_REQUIRING_PARAMETERS_MESSAGE);
        }
    }

    private boolean missingRequiredValues() {
        return Stream.of(sourceKey, sourceBucket, destinationKey, destinationKey).anyMatch(Objects::isNull);
    }

    private void performCopying(S3Client s3Client) throws MultipartCopyException {
        var headOfObjectToCopy = getHeadOfObjectToCopy(s3Client);
        var request = initiateMultiUploadRequest(headOfObjectToCopy.contentDisposition());
        var response = s3Client.createMultipartUpload(request);
        try {
            long currentPosition = 0;
            int partNumber = 1;
            long totalSize = headOfObjectToCopy.contentLength();
            while (currentPosition < totalSize) {
                long endPosition = Math.min(currentPosition + PARTITION_SIZE - 1, totalSize - 1);

                var uploadPartCopyRequest = createUploadPartCopyRequest(currentPosition, endPosition, response,
                                                                        partNumber);

                var uploadPartResponse = s3Client.uploadPartCopy(uploadPartCopyRequest);
                completedParts.add(createCompletedPart(partNumber, uploadPartResponse));
                currentPosition = endPosition + 1;
                partNumber++;
            }
            s3Client.completeMultipartUpload(createCompleteMultipartUploadRequest(response));
        } catch (Exception e) {
            logger.error(MULTIPART_UPLOAD_ERROR_MESSAGE, e.toString());
            s3Client.abortMultipartUpload(createAbortMultipartUploadRequest(response));
            throw MultipartCopyException.fromException(e);
        }
    }

    private UploadPartCopyRequest createUploadPartCopyRequest(long currentPosition, long endPosition,
                                                              CreateMultipartUploadResponse response, int partNumber) {
        return UploadPartCopyRequest.builder()
                   .destinationBucket(destinationBucket)
                   .destinationKey(destinationKey)
                   .sourceBucket(sourceBucket)
                   .sourceKey(sourceKey)
                   .copySourceRange(constructSourceRange(currentPosition, endPosition))
                   .uploadId(response.uploadId())
                   .partNumber(partNumber)
                   .build();
    }

    private AbortMultipartUploadRequest createAbortMultipartUploadRequest(CreateMultipartUploadResponse response) {
        return AbortMultipartUploadRequest.builder()
                   .bucket(destinationBucket)
                   .key(destinationKey)
                   .uploadId(response.uploadId())
                   .build();
    }

    private HeadObjectResponse getHeadOfObjectToCopy(S3Client s3Client) {
        var metadataRequest = HeadObjectRequest.builder().bucket(sourceBucket).key(sourceKey).build();
        return s3Client.headObject(metadataRequest);
    }

    private CompleteMultipartUploadRequest createCompleteMultipartUploadRequest(
        CreateMultipartUploadResponse response) {
        return CompleteMultipartUploadRequest.builder()
                   .bucket(destinationBucket)
                   .key(destinationKey)
                   .uploadId(response.uploadId())
                   .multipartUpload(completeMultipartUpload())
                   .build();
    }

    private CreateMultipartUploadRequest initiateMultiUploadRequest(String contentDisposition) {
        return CreateMultipartUploadRequest.builder()
                   .bucket(this.destinationBucket)
                   .key(this.destinationKey)
                   .contentDisposition(contentDisposition)
                   .build();
    }

    private static final class Builder {

        private String sourceKey;
        private String sourceBucket;
        private String destinationKey;
        private String destinationBucket;

        private Builder() {
        }

        public Builder withSourceKey(String sourceKey) {
            this.sourceKey = sourceKey;
            return this;
        }

        public Builder withSourceBucket(String sourceBucket) {
            this.sourceBucket = sourceBucket;
            return this;
        }

        public Builder withDestinationKey(String destinationKey) {
            this.destinationKey = destinationKey;
            return this;
        }

        public Builder withDestinationBucket(String destinationBucket) {
            this.destinationBucket = destinationBucket;
            return this;
        }

        public S3MultipartCopier build() {
            return new S3MultipartCopier(sourceKey, sourceBucket, destinationKey, destinationBucket);
        }
    }

    public static class MultipartCopyException extends RuntimeException {

        public static final String MULTIPART_COPY_EXCEPTION_MESSAGE = "Could not perform multipart copy!";

        public MultipartCopyException(String message, Exception e) {
            super(message, e);
        }

        public MultipartCopyException(String message) {
            super(message);
        }

        public static MultipartCopyException fromException(Exception exception) {
            return new MultipartCopyException(MULTIPART_COPY_EXCEPTION_MESSAGE, exception);
        }

        public static MultipartCopyException withMessage(String message) {
            return new MultipartCopyException(message);
        }
    }
}
