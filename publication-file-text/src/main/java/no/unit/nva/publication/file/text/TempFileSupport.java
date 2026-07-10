package no.unit.nva.publication.file.text;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared temp-file lifecycle utilities for the text extraction pipeline. */
final class TempFileSupport {

  private static final Logger LOGGER = LoggerFactory.getLogger(TempFileSupport.class);
  private static final String TEMP_DIRECTORY_SYSTEM_PROPERTY = "java.io.tmpdir";

  private TempFileSupport() {
    // NO-OP
  }

  static void deleteTempFile(Path tempFile) {
    if (isNull(tempFile)) {
      return;
    }
    try {
      Files.deleteIfExists(tempFile);
    } catch (IOException exception) {
      LOGGER.warn("Failed to delete temp file: {}", tempFile.getFileName(), exception);
    }
  }

  static void deleteStaleTempFiles(String filenamePrefix, String filenameSuffix) {
    var tempDirectory = Path.of(System.getProperty(TEMP_DIRECTORY_SYSTEM_PROPERTY));
    try (var tempDirectoryEntries = Files.list(tempDirectory)) {
      tempDirectoryEntries
          .filter(path -> hasPrefixAndSuffix(path, filenamePrefix, filenameSuffix))
          .forEach(TempFileSupport::deleteTempFile);
    } catch (IOException exception) {
      LOGGER.warn("Failed to sweep stale temp files", exception);
    }
  }

  private static boolean hasPrefixAndSuffix(
      Path path, String filenamePrefix, String filenameSuffix) {
    var filename = path.getFileName().toString();
    return filename.startsWith(filenamePrefix) && filename.endsWith(filenameSuffix);
  }
}
