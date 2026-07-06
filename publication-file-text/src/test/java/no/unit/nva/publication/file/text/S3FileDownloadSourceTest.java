package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

class S3FileDownloadSourceTest {

  private static final String BUCKET = "source-bucket";
  private static final String KEY = "publications/2024/doc.pdf";

  @Test
  @SuppressWarnings("unchecked")
  void shouldSendGetObjectRequestWithCorrectBucketAndKey() throws IOException {
    var s3Client = mock(S3Client.class);
    var capturedRequest = ArgumentCaptor.forClass(GetObjectRequest.class);
    var input = new ExtractionInput(BUCKET, KEY, "etag", "application/pdf");

    var path = new S3FileDownloadSource(s3Client).downloadToFile(input);

    verify(s3Client).getObject(capturedRequest.capture(), any(ResponseTransformer.class));
    assertThat(capturedRequest.getValue().bucket()).isEqualTo(BUCKET);
    assertThat(capturedRequest.getValue().key()).isEqualTo(KEY);
    assertThat(path).exists();
  }
}
