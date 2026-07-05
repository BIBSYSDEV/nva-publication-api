package no.unit.nva.publication.file.text;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

/** Resolves object metadata via a HeadObject call against the AWS S3 API. */
public final class S3ObjectMetadataSource implements ObjectMetadataSource {

  private final S3Client s3Client;

  public S3ObjectMetadataSource(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  @Override
  public ObjectMetadata fetchMetadata(String bucket, String key) {
    var response = s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
    return new ObjectMetadata(response.eTag(), response.contentType());
  }
}
