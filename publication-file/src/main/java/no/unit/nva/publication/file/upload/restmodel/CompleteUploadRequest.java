package no.unit.nva.publication.file.upload.restmodel;

import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import nva.commons.apigateway.exceptions.BadRequestException;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(name = InternalCompleteUploadRequest.TYPE, value = InternalCompleteUploadRequest.class),
    @JsonSubTypes.Type(name = ExternalCompleteUploadRequest.TYPE, value = ExternalCompleteUploadRequest.class)})
public interface CompleteUploadRequest {

    CompleteMultipartUploadRequest toCompleteMultipartUploadRequest(String bucketName);

    void validate() throws BadRequestException;
}
