package no.unit.nva.publication.file.text;

import java.util.List;

/**
 * Assembles the production set of text extractors. The dispatcher consults extractors in list order
 * — most specific format first, the plain-text generalist last — and routes each input to the first
 * one whose {@link TextExtractor#supports(String)} accepts the detected content type. The default
 * extractors support disjoint content types, so exactly one extractor handles each supported type;
 * the ordering is the documented contract for any future extractor whose supported set overlaps a
 * more generic one. Content types no extractor supports are flagged {@link
 * ExtractionFailureReason#UNSUPPORTED_FORMAT} by the dispatcher. Text-like content without a more
 * specific byte signature detects as {@code text/plain} and lands in the {@link
 * PlainTextExtractor}. The default PDF extractor flags image-only scans as {@link
 * ExtractionFailureReason#IMAGE_ONLY_CONTENT} via {@link FlaggingImageOnlyPdfProcessor}; wiring an
 * OCR-backed {@link ImageOnlyPdfProcessor} here is the single change needed to OCR them instead.
 */
public final class TextExtractors {

  private TextExtractors() {
    // NO-OP
  }

  public static List<TextExtractor> defaultExtractors() {
    return List.of(
        new PdfTextExtractor(new FlaggingImageOnlyPdfProcessor()),
        new WordTextExtractor(),
        new LatexTextExtractor(),
        new PlainTextExtractor());
  }
}
