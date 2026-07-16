package no.unit.nva.publication.file.text;

import java.nio.file.Path;
import java.util.Set;
import org.apache.tika.parser.ParseContext;

/** Extracts plain text from TeX and LaTeX documents. */
public final class LatexTextExtractor implements TextExtractor {

  private static final String TEX_CONTENT_TYPE = "application/x-tex";
  private static final String LATEX_CONTENT_TYPE = "application/x-latex";
  private static final Set<String> SUPPORTED_CONTENT_TYPES =
      Set.of(TEX_CONTENT_TYPE, LATEX_CONTENT_TYPE);

  @Override
  public boolean supports(String contentType) {
    return SUPPORTED_CONTENT_TYPES.contains(contentType);
  }

  @Override
  public ExtractionResult extract(ExtractionInput input, Path file) {
    return TikaExtraction.extract(input, file, ParseContext::new, TikaExtraction::parseError);
  }
}
