package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

  @Test
  @SuppressWarnings("unchecked")
  void shouldDeleteTempFileWhenGetObjectThrows() {
    var s3Client = mock(S3Client.class);
    doThrow(new RuntimeException("S3 error"))
        .when(s3Client)
        .getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));
    var input = new ExtractionInput(BUCKET, KEY, "etag", "application/pdf");
    var tempFilesBefore = countTempExtractionFiles();

    assertThatThrownBy(() -> new S3FileDownloadSource(s3Client).downloadToFile(input))
        .isInstanceOf(RuntimeException.class);

    assertThat(countTempExtractionFiles()).isEqualTo(tempFilesBefore);
  }

  private static long countTempExtractionFiles() {
    try (var stream = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
      return stream
          .filter(path -> path.getFileName().toString().startsWith("text-extraction-"))
          .count();
    } catch (IOException exception) {
      return 0;
    }
  }
}
