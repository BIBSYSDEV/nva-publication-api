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
}
