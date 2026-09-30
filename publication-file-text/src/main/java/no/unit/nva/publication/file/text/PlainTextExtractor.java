package no.unit.nva.publication.file.text;

import java.nio.file.Path;
import org.apache.tika.parser.ParseContext;

/**
 * Extracts text from plain-text files. Also the effective destination for text-like content whose
 * bytes carry no more specific signature (byte detection reports such content as {@code
 * text/plain}).
 */
public final class PlainTextExtractor implements TextExtractor {

  private static final String SUPPORTED_CONTENT_TYPE = "text/plain";

  @Override
  public boolean supports(String contentType) {
    return SUPPORTED_CONTENT_TYPE.equals(contentType);
  }

  @Override
  public ExtractionResult extract(ExtractionInput input, Path file) {
    return TikaExtraction.extract(input, file, ParseContext::new, TikaExtraction::parseError);
  }
}
