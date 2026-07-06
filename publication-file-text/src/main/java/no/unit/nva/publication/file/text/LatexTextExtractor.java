package no.unit.nva.publication.file.text;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.SAXException;

/** Extracts plain text from LaTeX documents. */
public final class LatexTextExtractor implements TextExtractor {

  private static final String SUPPORTED_CONTENT_TYPE = "application/x-latex";

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
      return new ExtractionResult.Extracted(input, TikaSupport.extractText(tempFile, new ParseContext()));
    } catch (TikaException | IOException | SAXException exception) {
      return new ExtractionResult.Flagged(
          input, ExtractionFailureReason.EXTRACTION_ERROR, LogSanitizer.sanitize(exception.getMessage()));
    } finally {
      TempFileSupport.deleteTempFile(tempFile);
    }
  }
}
