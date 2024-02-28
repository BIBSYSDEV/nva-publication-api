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
    private final String sourceS3Key;
    private final String sourceS3Bucket;
    private final String destinationS3Key;
    private final String destinationS3Bucket;
    private final List<CompletedPart> completedParts;

    private S3MultipartCopier(String sourceS3Key,
                              String sourceS3Bucket,
                              String destinationS3Key,
                              String destinationS3Bucket) {
        this.sourceS3Key = sourceS3Key;
        this.sourceS3Bucket = sourceS3Bucket;
        this.destinationS3Key = destinationS3Key;
        this.destinationS3Bucket = destinationS3Bucket;
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
        return builder().withSourceKey(this.sourceS3Key)
                   .withSourceBucket(this.sourceS3Bucket)
                   .withDestinationKey(this.destinationS3Key)
                   .withDestinationBucket(this.destinationS3Bucket);
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
        return Stream.of(sourceS3Key, sourceS3Bucket, destinationS3Key, destinationS3Key).anyMatch(Objects::isNull);
    }

    private void performCopying(S3Client s3Client) throws MultipartCopyException {
        var headOfObjectToCopy = getHeadOfObjectToCopy(s3Client);
        var request = initiateMultiUploadRequest(headOfObjectToCopy.contentDisposition());
        var response = s3Client.createMultipartUpload(request);
        try {
            long position = 0;
            int partNumber = 1;
            long totalSize = headOfObjectToCopy.contentLength();
            while (position < totalSize) {
                position = copyPartAndUpdatePosition(s3Client, position, totalSize, response, partNumber);
                partNumber++;
            }
            s3Client.completeMultipartUpload(createCompleteMultipartUploadRequest(response));
        } catch (Exception e) {
            logger.error(MULTIPART_UPLOAD_ERROR_MESSAGE, e.toString());
            s3Client.abortMultipartUpload(createAbortMultipartUploadRequest(response));
            throw MultipartCopyException.fromException(e);
        }
    }

    private long copyPartAndUpdatePosition(S3Client s3Client, long currentPosition, long totalSize,
                                           CreateMultipartUploadResponse response, int partNumber) {
        long endPosition = Math.min(currentPosition + PARTITION_SIZE - 1, totalSize - 1);

        var uploadPartCopyRequest =
            createUploadPartCopyRequest(currentPosition, endPosition, response, partNumber);

        var uploadPartResponse = s3Client.uploadPartCopy(uploadPartCopyRequest);
        completedParts.add(createCompletedPart(partNumber, uploadPartResponse));
        return ++endPosition;
    }

    private UploadPartCopyRequest createUploadPartCopyRequest(long currentPosition, long endPosition,
                                                              CreateMultipartUploadResponse response, int partNumber) {
        return UploadPartCopyRequest.builder()
                   .destinationBucket(destinationS3Bucket)
                   .destinationKey(destinationS3Key)
                   .sourceBucket(sourceS3Bucket)
                   .sourceKey(sourceS3Key)
                   .copySourceRange(constructSourceRange(currentPosition, endPosition))
                   .uploadId(response.uploadId())
                   .partNumber(partNumber)
                   .build();
    }

    private AbortMultipartUploadRequest createAbortMultipartUploadRequest(CreateMultipartUploadResponse response) {
        return AbortMultipartUploadRequest.builder()
                   .bucket(destinationS3Bucket)
                   .key(destinationS3Key)
                   .uploadId(response.uploadId())
                   .build();
    }

    private HeadObjectResponse getHeadOfObjectToCopy(S3Client s3Client) {
        var metadataRequest = HeadObjectRequest.builder().bucket(sourceS3Bucket).key(sourceS3Key).build();
        return s3Client.headObject(metadataRequest);
    }

    private CompleteMultipartUploadRequest createCompleteMultipartUploadRequest(
        CreateMultipartUploadResponse response) {
        return CompleteMultipartUploadRequest.builder()
                   .bucket(destinationS3Bucket)
                   .key(destinationS3Key)
                   .uploadId(response.uploadId())
                   .multipartUpload(completeMultipartUpload())
                   .build();
    }

    private CreateMultipartUploadRequest initiateMultiUploadRequest(String contentDisposition) {
        return CreateMultipartUploadRequest.builder()
                   .bucket(this.destinationS3Bucket)
                   .key(this.destinationS3Key)
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
