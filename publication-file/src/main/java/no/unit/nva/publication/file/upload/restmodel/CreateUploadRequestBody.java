package no.unit.nva.publication.file.upload.restmodel;

import static java.util.Objects.requireNonNull;

import java.util.UUID;
import no.unit.nva.publication.file.upload.Filename;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.exceptions.BadRequestException;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;

public record CreateUploadRequestBody(String filename, String size, String mediaType) {

  public CreateMultipartUploadRequest toCreateMultipartUploadRequest(String bucketName) {
    var key = UUID.randomUUID().toString();
    return CreateMultipartUploadRequest.builder()
        .bucket(bucketName)
        .key(key)
        .contentDisposition(Filename.toContentDispositionValue(filename()))
        .contentType(mediaType())
        .build();
  }

  public void validate() throws BadRequestException {
    try {
      requireNonNull(this.filename());
      requireNonNull(this.size());
      MediaType.parse(this.mediaType());
    } catch (Exception e) {
      throw new BadRequestException("Invalid input");
    }
  }
}
