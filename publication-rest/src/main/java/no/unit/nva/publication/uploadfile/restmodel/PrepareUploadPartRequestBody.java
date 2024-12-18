package no.unit.nva.publication.uploadfile.restmodel;

import static java.util.Objects.requireNonNull;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import nva.commons.apigateway.exceptions.BadRequestException;

public record PrepareUploadPartRequestBody(String uploadId, String key, String body, String number) {

    public static final String PARAMETER_UPLOAD_ID_KEY = "uploadId";
    public static final String PARAMETER_PART_NUMBER_KEY = "partNumber";

    public GeneratePresignedUrlRequest toGeneratePresignedUrlRequest(String bucketName) {
        var presignedUrlUploadRequest = new GeneratePresignedUrlRequest(bucketName, key()).withMethod(HttpMethod.PUT);
        presignedUrlUploadRequest.addRequestParameter(PARAMETER_UPLOAD_ID_KEY, uploadId());
        presignedUrlUploadRequest.addRequestParameter(PARAMETER_PART_NUMBER_KEY, number());
        return presignedUrlUploadRequest;
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
