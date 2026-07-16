package no.unit.nva.publication.file.text;

import java.nio.file.Path;
import java.util.Set;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.parser.ParseContext;

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

  @Override
  public boolean supports(String contentType) {
    return SUPPORTED_CONTENT_TYPES.contains(contentType);
  }

  @Override
  public ExtractionResult extract(ExtractionInput input, Path file) {
    return TikaExtraction.extract(
        input, file, ParseContext::new, WordTextExtractor::classifyFailure);
  }

  private static ExtractionResult classifyFailure(ExtractionInput input, Exception exception) {
    return ExceptionCauses.hasCauseOfType(
            exception,
            EncryptedDocumentException.class,
            org.apache.poi.EncryptedDocumentException.class)
        ? new ExtractionResult.Flagged(
            input, ExtractionFailureReason.PASSWORD_PROTECTED, PASSWORD_PROTECTED_DETAIL)
        : TikaExtraction.parseError(input, exception);
  }
}
