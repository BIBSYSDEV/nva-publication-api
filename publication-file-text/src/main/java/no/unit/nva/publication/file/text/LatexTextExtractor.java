package no.unit.nva.publication.file.text;

import static java.util.Objects.nonNull;

import java.util.Set;
import org.apache.tika.parser.ParseContext;

/** Extracts plain text from LaTeX and TeX documents. */
public final class LatexTextExtractor implements TextExtractor {

  private static final String LATEX_CONTENT_TYPE = "application/x-latex";
  private static final String TEX_CONTENT_TYPE = "application/x-tex";
  private static final String TEXT_TEX_CONTENT_TYPE = "text/x-tex";
  private static final Set<String> SUPPORTED_CONTENT_TYPES =
      Set.of(LATEX_CONTENT_TYPE, TEX_CONTENT_TYPE, TEXT_TEX_CONTENT_TYPE);

  private final FileDownloadSource downloadSource;

  public LatexTextExtractor(FileDownloadSource downloadSource) {
    this.downloadSource = downloadSource;
  }

  @Override
  public boolean supports(String contentType) {
    return nonNull(contentType) && SUPPORTED_CONTENT_TYPES.contains(contentType);
  }

  @Override
  public ExtractionResult extract(ExtractionInput input) {
    return TikaExtraction.extract(
        input, downloadSource, ParseContext::new, TikaExtraction::extractionError);
  }
}
