package no.unit.nva.publication.file.text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * Downloads S3 objects to fresh temp files with a single GetObject call. The returned ETag comes
 * from the same response that produced the bytes, so it always identifies the version that was
 * actually read.
 */
public final class S3FileDownloadSource implements FileDownloadSource {

  private static final String TEMP_FILE_PREFIX = "text-extraction-";
  private static final String TEMP_FILE_SUFFIX = ".bin";

  private final S3Client s3Client;

  public S3FileDownloadSource(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  @Override
  public DownloadedObject downloadToFile(String bucket, String key) throws IOException {
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

  private static Path createUniqueNonExistentTempPath() throws IOException {
    var tempFile = Files.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
    Files.delete(tempFile);
    return tempFile;
  }
}
