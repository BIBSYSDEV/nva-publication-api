package no.unit.nva.publication.file.text;

import java.io.IOException;
import java.nio.file.Path;

/** Port for downloading a source object to a local file for extraction. */
@FunctionalInterface
public interface FileDownloadSource {

  /**
   * Downloads the object described by {@code input} to a local temporary file and returns its path.
   * The caller is responsible for deleting the file when done.
   *
   * @param input coordinates of the source object
   * @return path to the downloaded file
   * @throws IOException if the temporary file cannot be created
   */
  Path downloadToFile(ExtractionInput input) throws IOException;
}
