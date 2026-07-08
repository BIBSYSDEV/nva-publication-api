package no.unit.nva.publication.file.text;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.SAXException;

/**
 * The shared Tika extraction flow: download the source object to a temp file, extract its text,
 * classify any parse failure, and always delete the temp file. Extractors differ only in the
 * content types they support, the {@link ParseContext} they parse with, and how they classify
 * failures.
 */
final class TikaExtraction {

  private TikaExtraction() {
    // NO-OP
  }

  static ExtractionResult extract(
      ExtractionInput input,
      FileDownloadSource downloadSource,
      Supplier<ParseContext> parseContextFactory,
      BiFunction<ExtractionInput, Exception, ExtractionResult> failureClassifier) {
    Path tempFile = null;
    try {
      tempFile = downloadSource.downloadToFile(input);
      return new ExtractionResult.Extracted(
          input, TikaSupport.extractText(tempFile, parseContextFactory.get()));
    } catch (TikaException | IOException | SAXException exception) {
      return failureClassifier.apply(input, exception);
    } finally {
      TempFileSupport.deleteTempFile(tempFile);
    }
  }

  static ExtractionResult extractionError(ExtractionInput input, Exception exception) {
    return new ExtractionResult.Flagged(
        input,
        ExtractionFailureReason.EXTRACTION_ERROR,
        LogSanitizer.sanitize(exception.getMessage()));
  }
}
