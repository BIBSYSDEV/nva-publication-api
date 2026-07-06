package no.unit.nva.publication.file.text;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.apache.poi.EncryptedDocumentException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Extracts plain text from Word documents in both OOXML (.docx) and legacy binary (.doc) formats.
 */
public final class WordTextExtractor implements TextExtractor {

  private static final Logger LOGGER = LoggerFactory.getLogger(WordTextExtractor.class);
  private static final String DOCX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final String DOC_CONTENT_TYPE = "application/msword";
  private static final Set<String> SUPPORTED_CONTENT_TYPES =
      Set.of(DOCX_CONTENT_TYPE, DOC_CONTENT_TYPE);
  private static final String PASSWORD_PROTECTED_DETAIL = "Password-protected document";
  private static final int UNLIMITED_CONTENT = -1;
  private static final AutoDetectParser PARSER = new AutoDetectParser();

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
      deleteTempFile(tempFile);
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
      if (current instanceof EncryptedDocumentException) {
        return true;
      }
    }
    return exception instanceof TikaException && isEncryptedMessage(exception.getMessage());
  }

  private static boolean isEncryptedMessage(String message) {
    return nonNull(message) && message.contains("encrypted");
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
