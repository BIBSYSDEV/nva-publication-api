package no.unit.nva.publication.file.text;

import java.util.List;

/**
 * Assembles the production set of text extractors. The dispatcher consults extractors in list order
 * and routes each input to the first one whose {@link TextExtractor#supports(String)} accepts the
 * content type; the default extractors support disjoint content types, so exactly one extractor
 * handles each supported type. Content types no extractor supports are flagged {@link
 * ExtractionFailureReason#UNSUPPORTED_FORMAT} by the dispatcher.
 */
public final class TextExtractors {

  private TextExtractors() {
    // NO-OP
  }

  public static List<TextExtractor> defaultExtractors(FileDownloadSource downloadSource) {
    return List.of(
        new PdfTextExtractor(downloadSource),
        new WordTextExtractor(downloadSource),
        new LatexTextExtractor(downloadSource));
  }
}
