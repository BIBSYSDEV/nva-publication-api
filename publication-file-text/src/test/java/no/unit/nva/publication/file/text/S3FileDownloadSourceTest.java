package no.unit.nva.publication.file.text;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
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
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class S3FileDownloadSourceTest {

  private static final String BUCKET = "source-bucket";
  private static final String KEY = "publications/2024/doc.pdf";
  private static final String ETAG = "\"abc123\"";
  private static final String FILE_CONTENT = "%PDF-1.4 fake content";

  @Test
  void shouldDownloadObjectContentToReadableTempFile() throws IOException {
    var fakeS3Client = new FakeS3Client();
    new S3Driver(fakeS3Client, BUCKET).insertFile(UnixPath.of(KEY), FILE_CONTENT);

    var downloadedObject = new S3FileDownloadSource(fakeS3Client).downloadToFile(BUCKET, KEY);

    assertThat(downloadedObject.path()).hasContent(FILE_CONTENT);
    Files.deleteIfExists(downloadedObject.path());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldReturnEtagFromGetObjectResponseAndRequestCorrectObject() throws IOException {
    var s3Client = mock(S3Client.class);
    var capturedRequest = ArgumentCaptor.forClass(GetObjectRequest.class);
    when(s3Client.getObject(capturedRequest.capture(), any(ResponseTransformer.class)))
        .thenAnswer(
            invocation -> {
              ResponseTransformer<GetObjectResponse, GetObjectResponse> transformer =
                  invocation.getArgument(1);
              return transformer.transform(
                  GetObjectResponse.builder().eTag(ETAG).build(),
                  AbortableInputStream.create(
                      new ByteArrayInputStream(FILE_CONTENT.getBytes(UTF_8))));
            });

    var downloadedObject = new S3FileDownloadSource(s3Client).downloadToFile(BUCKET, KEY);

    assertThat(downloadedObject.etag()).isEqualTo(ETAG);
    assertThat(downloadedObject.path()).hasContent(FILE_CONTENT);
    assertThat(capturedRequest.getValue().bucket()).isEqualTo(BUCKET);
    assertThat(capturedRequest.getValue().key()).isEqualTo(KEY);
    Files.deleteIfExists(downloadedObject.path());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldDeleteTempFileWhenGetObjectThrows() {
    var s3Client = mock(S3Client.class);
    doThrow(new IllegalStateException("S3 error"))
        .when(s3Client)
        .getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));
    var tempFilesBefore = countTempExtractionFiles();

    assertThatThrownBy(() -> new S3FileDownloadSource(s3Client).downloadToFile(BUCKET, KEY))
        .isInstanceOf(IllegalStateException.class);

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
