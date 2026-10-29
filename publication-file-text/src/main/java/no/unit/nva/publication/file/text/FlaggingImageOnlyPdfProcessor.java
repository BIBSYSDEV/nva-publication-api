package no.unit.nva.publication.file.text;

import java.nio.file.Path;

/**
 * Default {@link ImageOnlyPdfProcessor}: records the scan evidence as an {@link
 * ExtractionFailureReason#IMAGE_ONLY_CONTENT} flag, so image-only documents remain enumerable OCR
 * candidates until an OCR-backed processor replaces this adapter.
 */
public final class FlaggingImageOnlyPdfProcessor implements ImageOnlyPdfProcessor {

  @Override
  public ExtractionResult process(
      ExtractionInput input, Path file, PdfScanFingerprint fingerprint) {
    return new ExtractionResult.Flagged(
        input, ExtractionFailureReason.IMAGE_ONLY_CONTENT, fingerprint.describe());
  }
}
