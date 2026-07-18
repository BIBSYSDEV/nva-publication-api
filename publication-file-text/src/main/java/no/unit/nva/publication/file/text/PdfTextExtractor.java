package no.unit.nva.publication.file.text;

import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParserConfig;

/**
 * Extracts plain text from PDF files using Apache Tika and PDFBox. PDFs that {@link PdfScanSupport}
 * proves to be image-only scans are not parsed for text at all — they are routed to the configured
 * {@link ImageOnlyPdfProcessor}, which decides whether they are flagged as OCR candidates or
 * OCR-processed.
 */
public final class PdfTextExtractor implements TextExtractor {

  private static final String SUPPORTED_CONTENT_TYPE = "application/pdf";
  private static final String PASSWORD_PROTECTED_DETAIL = "Password-protected PDF";

  private final ImageOnlyPdfProcessor imageOnlyPdfProcessor;

  public PdfTextExtractor(ImageOnlyPdfProcessor imageOnlyPdfProcessor) {
    this.imageOnlyPdfProcessor = imageOnlyPdfProcessor;
  }

  @Override
  public boolean supports(String contentType) {
    return SUPPORTED_CONTENT_TYPE.equals(contentType);
  }

  @Override
  public ExtractionResult extract(ExtractionInput input, Path file) {
    return PdfScanSupport.detectImageOnlyPdf(file)
        .map(fingerprint -> imageOnlyPdfProcessor.process(input, file, fingerprint))
        .orElseGet(
            () ->
                TikaExtraction.extract(
                    input,
                    file,
                    PdfTextExtractor::createParseContext,
                    PdfTextExtractor::classifyFailure));
  }

  private static ExtractionResult classifyFailure(ExtractionInput input, Exception exception) {
    return ExceptionCauses.hasCauseOfType(exception, InvalidPasswordException.class)
        ? new ExtractionResult.Flagged(
            input, ExtractionFailureReason.PASSWORD_PROTECTED, PASSWORD_PROTECTED_DETAIL)
        : TikaExtraction.parseError(input, exception);
  }

  private static ParseContext createParseContext() {
    var context = new ParseContext();
    var pdfConfig = new PDFParserConfig();
    pdfConfig.setSortByPosition(true);
    context.set(PDFParserConfig.class, pdfConfig);
    return context;
  }
}
