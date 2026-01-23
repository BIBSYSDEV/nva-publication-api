package no.unit.nva.publication.file.upload.restmodel;

import static java.util.Objects.requireNonNull;
import nva.commons.apigateway.exceptions.BadRequestException;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;

public record ListPartsRequestBody(String uploadId, String key) {

    public void validate() throws BadRequestException {
        try {
            requireNonNull(this.key());
            requireNonNull(this.uploadId());
        } catch (Exception e) {
            throw new BadRequestException("Invalid input");
        }
    }

    public ListPartsRequest toListPartsRequest(String bucketName) {
        return ListPartsRequest.builder()
                   .bucket(bucketName)
                   .key(key())
                   .uploadId(uploadId())
                   .build();
    }
}
