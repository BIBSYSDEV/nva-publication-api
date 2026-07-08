package no.unit.nva.publication.file.text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * Downloads S3 objects to fresh temp files. The download is conditional on the ETag recorded in the
 * {@link ExtractionInput}: if the source object was replaced after its metadata was resolved, S3
 * rejects the read and the message is retried with fresh metadata.
 */
public final class S3FileDownloadSource implements FileDownloadSource {

  private static final String TEMP_FILE_PREFIX = "text-extraction-";
  private static final String TEMP_FILE_SUFFIX = ".bin";

  private final S3Client s3Client;

  public S3FileDownloadSource(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  @Override
  public Path downloadToFile(ExtractionInput input) throws IOException {
    var tempFile = createUniqueNonExistentTempPath();
    try {
      s3Client.getObject(
          GetObjectRequest.builder()
              .bucket(input.sourceBucket())
              .key(input.sourceKey())
              .ifMatch(input.sourceEtag())
              .build(),
          ResponseTransformer.toFile(tempFile));
    } catch (RuntimeException exception) {
      TempFileSupport.deleteTempFile(tempFile);
      throw exception;
    }
    return tempFile;
  }

  private static Path createUniqueNonExistentTempPath() throws IOException {
    var tempFile = Files.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
    Files.delete(tempFile);
    return tempFile;
  }
}
