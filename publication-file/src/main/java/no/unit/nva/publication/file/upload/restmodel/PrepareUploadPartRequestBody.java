package no.unit.nva.publication.file.upload.restmodel;

import static java.util.Objects.requireNonNull;
import nva.commons.apigateway.exceptions.BadRequestException;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

public record PrepareUploadPartRequestBody(String uploadId, String key, String body, String number) {

    public UploadPartRequest toUploadPartRequest(String bucketName) {
        return UploadPartRequest.builder()
                   .bucket(bucketName)
                   .key(key())
                   .uploadId(uploadId())
                   .partNumber(Integer.parseInt(number()))
                   .build();
    }

    public void validate() throws BadRequestException {
        try {
            requireNonNull(this.key());
            requireNonNull(this.uploadId());
            requireNonNull(this.number());
        } catch (Exception e) {
            throw new BadRequestException("Invalid input");
        }
    }
}
