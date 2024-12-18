package no.unit.nva.publication.uploadfile.restmodel;

import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;

public record CreateUploadResponseBody(String uploadId, String key) {

    public static CreateUploadResponseBody fromInitiateMultipartUploadResult(InitiateMultipartUploadResult result) {
        return new CreateUploadResponseBody(result.getUploadId(), result.getKey());
    }
}
