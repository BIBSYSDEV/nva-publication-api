package no.unit.nva.publication.file.text;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.SAXException;

/**
 * The shared Tika extraction flow over an already-downloaded local file. Every exception thrown by
 * the parse is deterministic for the given bytes (Tika wraps parser runtime failures, and local I/O
 * errors are treated the same way), so failures are classified into {@link
 * ExtractionResult.Flagged} results rather than thrown; retrying would fail identically. Extractors
 * differ only in the content types they support, the {@link ParseContext} they parse with, and how
 * they classify failures.
 */
final class TikaExtraction {

  private TikaExtraction() {
    // NO-OP
  }

  static ExtractionResult extract(
      ExtractionInput input,
      Path file,
      Supplier<ParseContext> parseContextFactory,
      BiFunction<ExtractionInput, Exception, ExtractionResult> failureClassifier) {
    try {
      var extractedText = TikaSupport.extractText(file, parseContextFactory.get());
      return new ExtractionResult.Extracted(input, extractedText.text(), extractedText.truncated());
    } catch (TikaException | IOException | SAXException exception) {
      return failureClassifier.apply(input, exception);
    }
  }

  static ExtractionResult parseError(ExtractionInput input, Exception exception) {
    return new ExtractionResult.Flagged(
        input, ExtractionFailureReason.PARSE_ERROR, LogSanitizer.sanitize(exception.getMessage()));
  }
}
