package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

class S3FileDownloadSourceTest {

  private static final String BUCKET = "source-bucket";
  private static final String KEY = "publications/2024/doc.pdf";
  private static final String ETAG = "\"abc123\"";
  private static final String PDF_CONTENT_TYPE = "application/pdf";
  private static final String FILE_CONTENT = "%PDF-1.4 fake content";
  private static final ExtractionInput SOME_INPUT =
      new ExtractionInput(BUCKET, KEY, ETAG, PDF_CONTENT_TYPE);

  @Test
  void shouldDownloadObjectContentToReadableTempFile() throws IOException {
    var fakeS3Client = new FakeS3Client();
    new S3Driver(fakeS3Client, BUCKET).insertFile(UnixPath.of(KEY), FILE_CONTENT);

    var path = new S3FileDownloadSource(fakeS3Client).downloadToFile(SOME_INPUT);

    assertThat(path).hasContent(FILE_CONTENT);
    Files.deleteIfExists(path);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldSendGetObjectRequestWithBucketKeyAndEtagCondition() throws IOException {
    var s3Client = mock(S3Client.class);
    var capturedRequest = ArgumentCaptor.forClass(GetObjectRequest.class);

    var path = new S3FileDownloadSource(s3Client).downloadToFile(SOME_INPUT);

    verify(s3Client).getObject(capturedRequest.capture(), any(ResponseTransformer.class));
    assertThat(capturedRequest.getValue().bucket()).isEqualTo(BUCKET);
    assertThat(capturedRequest.getValue().key()).isEqualTo(KEY);
    assertThat(capturedRequest.getValue().ifMatch()).isEqualTo(ETAG);
    Files.deleteIfExists(path);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldDeleteTempFileWhenGetObjectThrows() {
    var s3Client = mock(S3Client.class);
    doThrow(new RuntimeException("S3 error"))
        .when(s3Client)
        .getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));
    var tempFilesBefore = countTempExtractionFiles();

    assertThatThrownBy(() -> new S3FileDownloadSource(s3Client).downloadToFile(SOME_INPUT))
        .isInstanceOf(RuntimeException.class);

    assertThat(countTempExtractionFiles()).isEqualTo(tempFilesBefore);
  }

  private static long countTempExtractionFiles() {
    try (var stream = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
      return stream
          .filter(path -> path.getFileName().toString().startsWith("text-extraction-"))
          .count();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
