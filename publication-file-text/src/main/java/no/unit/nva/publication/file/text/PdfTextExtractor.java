package no.unit.nva.publication.file.text;

import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParserConfig;

/** Extracts plain text from PDF files using Apache Tika and PDFBox. */
public final class PdfTextExtractor implements TextExtractor {

  private static final String SUPPORTED_CONTENT_TYPE = "application/pdf";
  private static final String PASSWORD_PROTECTED_DETAIL = "Password-protected PDF";

  private final FileDownloadSource downloadSource;

  public PdfTextExtractor(FileDownloadSource downloadSource) {
    this.downloadSource = downloadSource;
  }

  @Override
  public boolean supports(String contentType) {
    return SUPPORTED_CONTENT_TYPE.equals(contentType);
  }

  @Override
  public ExtractionResult extract(ExtractionInput input) {
    return TikaExtraction.extract(
        input,
        downloadSource,
        PdfTextExtractor::createParseContext,
        PdfTextExtractor::classifyFailure);
  }

  private static ExtractionResult classifyFailure(ExtractionInput input, Exception exception) {
    return ExceptionCauses.hasCauseOfType(exception, InvalidPasswordException.class)
        ? new ExtractionResult.Flagged(
            input, ExtractionFailureReason.PASSWORD_PROTECTED, PASSWORD_PROTECTED_DETAIL)
        : TikaExtraction.extractionError(input, exception);
  }

  private static ParseContext createParseContext() {
    var context = new ParseContext();
    var pdfConfig = new PDFParserConfig();
    pdfConfig.setSortByPosition(true);
    context.set(PDFParserConfig.class, pdfConfig);
    return context;
  }
}
