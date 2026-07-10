package no.unit.nva.publication.file.text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

/**
 * Downloads S3 objects to fresh temp files with a single GetObject call. The returned ETag comes
 * from the same response that produced the bytes, so it always identifies the version that was
 * actually read. Objects whose declared size exceeds {@code MAX_SOURCE_OBJECT_BYTES} (sized to fit
 * the function's 10 GiB ephemeral storage with headroom for extractor scratch space) are rejected
 * with {@link FileTooLargeException} before any bytes are transferred, so an oversized object costs
 * one metadata call instead of repeated full-size transfers that can never succeed. Before each
 * download, temp files left behind by interrupted earlier invocations in the same execution
 * environment are deleted, so a crash or timeout cannot exhaust local storage permanently.
 */
public final class S3FileDownloadSource implements FileDownloadSource {

  static final long MAX_SOURCE_OBJECT_BYTES = 9L * 1024 * 1024 * 1024;

  private static final String TEMP_FILE_PREFIX = "text-extraction-";
  private static final String TEMP_FILE_SUFFIX = ".bin";

  private final S3Client s3Client;

  public S3FileDownloadSource(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  @Override
  public DownloadedObject downloadToFile(String bucket, String key) throws IOException {
    TempFileSupport.deleteStaleTempFiles(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
    rejectOversizedObject(bucket, key);
    var tempFile = createUniqueNonExistentTempPath();
    try {
      var response =
          s3Client.getObject(
              GetObjectRequest.builder().bucket(bucket).key(key).build(),
              ResponseTransformer.toFile(tempFile));
      return new DownloadedObject(tempFile, response.eTag());
    } catch (RuntimeException exception) {
      TempFileSupport.deleteTempFile(tempFile);
      throw exception;
    }
  }

  private void rejectOversizedObject(String bucket, String key) throws FileTooLargeException {
    var headResponse =
        s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
    if (headResponse.contentLength() > MAX_SOURCE_OBJECT_BYTES) {
      throw new FileTooLargeException(
          headResponse.contentLength(), MAX_SOURCE_OBJECT_BYTES, headResponse.eTag());
    }
  }

  private static Path createUniqueNonExistentTempPath() throws IOException {
    var tempFile = Files.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
    Files.delete(tempFile);
    return tempFile;
  }
}
