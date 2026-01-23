package no.unit.nva.publication.file.upload.restmodel;

import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;

public record CreateUploadResponseBody(String uploadId, String key) {

    public static CreateUploadResponseBody fromCreateMultipartUploadResponse(CreateMultipartUploadResponse response) {
        return new CreateUploadResponseBody(response.uploadId(), response.key());
    }
}
