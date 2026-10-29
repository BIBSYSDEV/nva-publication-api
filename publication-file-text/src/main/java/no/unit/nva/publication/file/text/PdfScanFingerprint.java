package no.unit.nva.publication.file.text;

/**
 * Structural evidence that a PDF is an image-only scan: it has pages, at least one embedded image,
 * and no font resources anywhere reachable. PDF text cannot be drawn without a font, so the absence
 * of fonts proves the absence of a text layer. Produced by {@link PdfScanSupport} and consumed by
 * {@link ImageOnlyPdfProcessor} implementations; {@link #describe()} is the canonical
 * human-readable statement of the evidence, suitable as flag detail.
 */
public record PdfScanFingerprint(int pageCount, int pagesWithImages) {

  private static final String DESCRIPTION_TEMPLATE =
      "Image-only PDF: %d pages, no embedded fonts, images on %d pages; text requires OCR";

  public String describe() {
    return DESCRIPTION_TEMPLATE.formatted(pageCount, pagesWithImages);
  }
}
