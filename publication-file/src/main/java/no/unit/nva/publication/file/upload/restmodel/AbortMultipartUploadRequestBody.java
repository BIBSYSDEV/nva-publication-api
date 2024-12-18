package no.unit.nva.publication.file.upload.restmodel;

import static java.util.Objects.requireNonNull;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import nva.commons.apigateway.exceptions.BadRequestException;

public record AbortMultipartUploadRequestBody(String uploadId, String key) {

    public AbortMultipartUploadRequest toAbortMultipartUploadRequest(String bucketName) {
        return new AbortMultipartUploadRequest(bucketName, key(), uploadId());
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
