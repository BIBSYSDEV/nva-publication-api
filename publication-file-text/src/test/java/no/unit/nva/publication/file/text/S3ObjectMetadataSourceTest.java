package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

class S3ObjectMetadataSourceTest {

  private static final String SOME_ETAG = "\"d41d8cd98f00b204e9800998ecf8427e\"";
  private static final String SOME_CONTENT_TYPE = "application/pdf";

  @Test
  void shouldReturnEtagAndContentTypeFromHeadObjectResponse() {
    var s3Client = mock(S3Client.class);
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenReturn(
            HeadObjectResponse.builder().eTag(SOME_ETAG).contentType(SOME_CONTENT_TYPE).build());

    var metadata = new S3ObjectMetadataSource(s3Client).fetchMetadata("bucket", "key");

    assertThat(metadata).isEqualTo(new ObjectMetadata(SOME_ETAG, SOME_CONTENT_TYPE));
  }
}
