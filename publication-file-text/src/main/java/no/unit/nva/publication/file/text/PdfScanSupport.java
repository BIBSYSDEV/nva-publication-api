package no.unit.nva.publication.file.text;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects image-only (scanned) PDFs from document structure. A fingerprint is returned only when
 * the document provably has no text layer: no font resources on any page or nested form XObject —
 * PDF text cannot be drawn without a font — no interactive form fields, and at least one embedded
 * image. Detection is best-effort: a document that cannot be opened or inspected (for example an
 * encrypted one) yields an empty result, and the caller proceeds with ordinary text extraction,
 * which classifies such documents through its own failure handling.
 */
final class PdfScanSupport {

  private static final Logger LOGGER = LoggerFactory.getLogger(PdfScanSupport.class);

  private PdfScanSupport() {
    // NO-OP
  }

  static Optional<PdfScanFingerprint> detectImageOnlyPdf(Path file) {
    try (var document = Loader.loadPDF(file.toFile())) {
      return detectImageOnlyPdf(document);
    } catch (IOException | RuntimeException exception) {
      LOGGER.warn("Scan detection failed, continuing with text extraction", exception);
      return Optional.empty();
    }
  }

  private static Optional<PdfScanFingerprint> detectImageOnlyPdf(PDDocument document)
      throws IOException {
    if (document.getNumberOfPages() == 0
        || hasInteractiveFormFields(document)
        || anyPageHasFonts(document)) {
      return Optional.empty();
    }
    var pagesWithImages = countPagesWithImages(document);
    return pagesWithImages > 0
        ? Optional.of(new PdfScanFingerprint(document.getNumberOfPages(), pagesWithImages))
        : Optional.empty();
  }

  private static boolean hasInteractiveFormFields(PDDocument document) {
    var acroForm = document.getDocumentCatalog().getAcroForm();
    return nonNull(acroForm) && !acroForm.getFields().isEmpty();
  }

  private static boolean anyPageHasFonts(PDDocument document) throws IOException {
    var visitedForms = new HashSet<COSStream>();
    for (var page : document.getPages()) {
      if (containsFonts(page.getResources(), visitedForms)) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsFonts(PDResources resources, Set<COSStream> visitedForms)
      throws IOException {
    if (isNull(resources)) {
      return false;
    }
    if (resources.getFontNames().iterator().hasNext()) {
      return true;
    }
    for (var xObjectName : resources.getXObjectNames()) {
      if (isFormContainingFonts(resources.getXObject(xObjectName), visitedForms)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isFormContainingFonts(PDXObject xObject, Set<COSStream> visitedForms)
      throws IOException {
    return xObject instanceof PDFormXObject form
        && visitedForms.add(form.getCOSObject())
        && containsFonts(form.getResources(), visitedForms);
  }

  private static int countPagesWithImages(PDDocument document) throws IOException {
    var pagesWithImages = 0;
    for (var page : document.getPages()) {
      if (containsImages(page.getResources(), new HashSet<>())) {
        pagesWithImages++;
      }
    }
    return pagesWithImages;
  }

  private static boolean containsImages(PDResources resources, Set<COSStream> visitedForms)
      throws IOException {
    if (isNull(resources)) {
      return false;
    }
    for (var xObjectName : resources.getXObjectNames()) {
      if (isOrContainsImage(resources.getXObject(xObjectName), visitedForms)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isOrContainsImage(PDXObject xObject, Set<COSStream> visitedForms)
      throws IOException {
    return switch (xObject) {
      case PDImageXObject ignored -> true;
      case PDFormXObject form when visitedForms.add(form.getCOSObject()) ->
          containsImages(form.getResources(), visitedForms);
      default -> false;
    };
  }
}
