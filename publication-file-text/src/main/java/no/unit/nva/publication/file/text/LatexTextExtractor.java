package no.unit.nva.publication.file.text;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/** Extracts plain text from LaTeX documents. */
public final class LatexTextExtractor implements TextExtractor {

  private static final Logger LOGGER = LoggerFactory.getLogger(LatexTextExtractor.class);
  private static final String SUPPORTED_CONTENT_TYPE = "application/x-latex";
  private static final int UNLIMITED_CONTENT = -1;
  private static final AutoDetectParser PARSER = new AutoDetectParser();

  private final FileDownloadSource downloadSource;

  public LatexTextExtractor(FileDownloadSource downloadSource) {
    this.downloadSource = downloadSource;
  }

  @Override
  public boolean supports(String contentType) {
    return SUPPORTED_CONTENT_TYPE.equals(contentType);
  }

  @Override
  public ExtractionResult extract(ExtractionInput input) {
    Path tempFile = null;
    try {
      tempFile = downloadSource.downloadToFile(input);
      return new ExtractionResult.Extracted(input, extractText(tempFile));
    } catch (TikaException | IOException | SAXException exception) {
      return new ExtractionResult.Flagged(
          input, ExtractionFailureReason.EXTRACTION_ERROR, exception.getMessage());
    } finally {
      deleteTempFile(tempFile);
    }
  }

  private static String extractText(Path file) throws TikaException, IOException, SAXException {
    var handler = new BodyContentHandler(UNLIMITED_CONTENT);
    try (var stream = TikaInputStream.get(file)) {
      PARSER.parse(stream, handler, new Metadata(), new ParseContext());
    }
    return handler.toString();
  }

  private static void deleteTempFile(Path tempFile) {
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
