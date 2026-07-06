package no.unit.nva.publication.file.text;

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

/**
 * Extracts plain text from Word documents in both OOXML (.docx) and legacy binary (.doc) formats.
 */
public final class WordTextExtractor implements TextExtractor {

  private static final String DOCX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final String DOC_CONTENT_TYPE = "application/msword";
  private static final Set<String> SUPPORTED_CONTENT_TYPES =
      Set.of(DOCX_CONTENT_TYPE, DOC_CONTENT_TYPE);
  private static final String PASSWORD_PROTECTED_DETAIL = "Password-protected document";

  private final FileDownloadSource downloadSource;

  public WordTextExtractor(FileDownloadSource downloadSource) {
    this.downloadSource = downloadSource;
  }

  @Override
  public boolean supports(String contentType) {
    return SUPPORTED_CONTENT_TYPES.contains(contentType);
  }

  @Override
  public ExtractionResult extract(ExtractionInput input) {
    Path tempFile = null;
    try {
      tempFile = downloadSource.downloadToFile(input);
      return new ExtractionResult.Extracted(input, extractText(tempFile));
    } catch (TikaException | IOException | SAXException exception) {
      return classifyFailure(input, exception);
    } finally {
      TempFileSupport.deleteTempFile(tempFile);
    }
  }

  private static ExtractionResult classifyFailure(ExtractionInput input, Exception exception) {
    return isPasswordProtected(exception)
        ? new ExtractionResult.Flagged(
            input, ExtractionFailureReason.PASSWORD_PROTECTED, PASSWORD_PROTECTED_DETAIL)
        : new ExtractionResult.Flagged(
            input, ExtractionFailureReason.EXTRACTION_ERROR, exception.getMessage());
  }

  private static boolean isPasswordProtected(Exception exception) {
    for (Throwable current = exception; nonNull(current); current = current.getCause()) {
      if (current instanceof EncryptedDocumentException
          || current instanceof org.apache.poi.EncryptedDocumentException) {
        return true;
      }
    }
    return false;
  }

  private static String extractText(Path file) throws TikaException, IOException, SAXException {
    var handler = new BodyContentHandler(TikaSupport.UNLIMITED_CONTENT);
    try (var stream = TikaInputStream.get(file)) {
      TikaSupport.PARSER.parse(stream, handler, new Metadata(), new ParseContext());
    }
    return handler.toString();
  }
}
