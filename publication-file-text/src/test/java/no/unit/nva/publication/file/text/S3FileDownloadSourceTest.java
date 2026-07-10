package no.unit.nva.publication.file.text;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

class S3FileDownloadSourceTest {

  private static final String BUCKET = "source-bucket";
  private static final String KEY = "publications/2024/doc.pdf";
  private static final String ETAG = "\"abc123\"";
  private static final String FILE_CONTENT = "%PDF-1.4 fake content";
  private static final String TEMP_FILE_PREFIX = "text-extraction-";
  private static final String TEMP_FILE_SUFFIX = ".bin";
  private static final long OVERSIZED_OBJECT_BYTES =
      S3FileDownloadSource.MAX_SOURCE_OBJECT_BYTES + 1;

  @Test
  @SuppressWarnings("unchecked")
  void shouldThrowFileTooLargeWithoutTransferringWhenObjectExceedsSizeLimit() {
    var s3Client = mock(S3Client.class);
    stubObjectSize(s3Client, OVERSIZED_OBJECT_BYTES);
    var tempFilesBefore = countTempExtractionFiles();

    assertThatThrownBy(() -> new S3FileDownloadSource(s3Client).downloadToFile(BUCKET, KEY))
        .isInstanceOfSatisfying(
            FileTooLargeException.class,
            exception -> {
              assertThat(exception.getObjectSizeBytes()).isEqualTo(OVERSIZED_OBJECT_BYTES);
              assertThat(exception.getLimitBytes())
                  .isEqualTo(S3FileDownloadSource.MAX_SOURCE_OBJECT_BYTES);
              assertThat(exception.getEtag()).isEqualTo(ETAG);
            });

    verify(s3Client, never())
        .getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));
    assertThat(countTempExtractionFiles()).isEqualTo(tempFilesBefore);
  }

  @Test
  void shouldSweepStaleTempFilesFromEarlierInvocationsBeforeDownloading() throws IOException {
    var staleTempFile = Files.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
    var fakeS3Client = fakeS3ClientReportingObjectSize(FILE_CONTENT.length());
    new S3Driver(fakeS3Client, BUCKET).insertFile(UnixPath.of(KEY), FILE_CONTENT);

    var downloadedObject = new S3FileDownloadSource(fakeS3Client).downloadToFile(BUCKET, KEY);

    assertThat(staleTempFile).doesNotExist();
    assertThat(downloadedObject.path()).hasContent(FILE_CONTENT);
    Files.deleteIfExists(downloadedObject.path());
  }

  @Test
  void shouldDownloadObjectContentToReadableTempFile() throws IOException {
    var fakeS3Client = fakeS3ClientReportingObjectSize(FILE_CONTENT.length());
    new S3Driver(fakeS3Client, BUCKET).insertFile(UnixPath.of(KEY), FILE_CONTENT);

    var downloadedObject = new S3FileDownloadSource(fakeS3Client).downloadToFile(BUCKET, KEY);

    assertThat(downloadedObject.path()).hasContent(FILE_CONTENT);
    Files.deleteIfExists(downloadedObject.path());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldReturnEtagFromGetObjectResponseAndRequestCorrectObject() throws IOException {
    var s3Client = mock(S3Client.class);
    stubObjectSize(s3Client, FILE_CONTENT.length());
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
    stubObjectSize(s3Client, FILE_CONTENT.length());
    doThrow(new IllegalStateException("S3 error"))
        .when(s3Client)
        .getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));
    var tempFilesBefore = countTempExtractionFiles();

    assertThatThrownBy(() -> new S3FileDownloadSource(s3Client).downloadToFile(BUCKET, KEY))
        .isInstanceOf(IllegalStateException.class);

    assertThat(countTempExtractionFiles()).isEqualTo(tempFilesBefore);
  }

  private static void stubObjectSize(S3Client s3Client, long objectSizeBytes) {
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenReturn(HeadObjectResponse.builder().contentLength(objectSizeBytes).eTag(ETAG).build());
  }

  private static FakeS3Client fakeS3ClientReportingObjectSize(long objectSizeBytes) {
    return new FakeS3Client() {
      @Override
      public HeadObjectResponse headObject(HeadObjectRequest request) {
        return HeadObjectResponse.builder().contentLength(objectSizeBytes).eTag(ETAG).build();
      }
    };
  }

  private static long countTempExtractionFiles() {
    try (var stream = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
      return stream
          .filter(path -> path.getFileName().toString().startsWith(TEMP_FILE_PREFIX))
          .count();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
