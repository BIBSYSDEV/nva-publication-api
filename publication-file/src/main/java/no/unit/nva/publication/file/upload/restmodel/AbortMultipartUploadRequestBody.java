package no.unit.nva.publication.file.upload.restmodel;

import static java.util.Objects.requireNonNull;
import nva.commons.apigateway.exceptions.BadRequestException;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;

public record AbortMultipartUploadRequestBody(String uploadId, String key) {

    public AbortMultipartUploadRequest toAbortMultipartUploadRequest(String bucketName) {
        return AbortMultipartUploadRequest.builder()
                   .bucket(bucketName)
                   .key(key())
                   .uploadId(uploadId())
                   .build();
    }

    public void validate() throws BadRequestException {
        try {
            requireNonNull(uploadId());
            requireNonNull(key());
        } catch (Exception e) {
            throw new BadRequestException("Invalid input");
        }
    }
}
