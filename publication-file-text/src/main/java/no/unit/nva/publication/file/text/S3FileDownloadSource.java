package no.unit.nva.publication.file.text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/** Downloads S3 objects to a local temp file via the AWS SDK. */
public final class S3FileDownloadSource implements FileDownloadSource {

  private static final String TEMP_FILE_PREFIX = "text-extraction-";
  private static final String TEMP_FILE_SUFFIX = ".bin";

  private final S3Client s3Client;

  public S3FileDownloadSource(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  @Override
  public Path downloadToFile(ExtractionInput input) throws IOException {
    var tempFile = Files.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
    try {
      s3Client.getObject(
          GetObjectRequest.builder().bucket(input.sourceBucket()).key(input.sourceKey()).build(),
          ResponseTransformer.toFile(tempFile));
    } catch (RuntimeException exception) {
      TempFileSupport.deleteTempFile(tempFile);
      throw exception;
    }
    return tempFile;
  }
}
