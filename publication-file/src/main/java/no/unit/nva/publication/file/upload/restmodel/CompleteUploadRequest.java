package no.unit.nva.publication.file.upload.restmodel;

import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import nva.commons.apigateway.exceptions.BadRequestException;

public interface CompleteUploadRequest {

    CompleteMultipartUploadRequest toCompleteMultipartUploadRequest(String bucketName);

    void validate() throws BadRequestException;
}
