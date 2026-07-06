package no.unit.nva.publication.file.text;

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.xml.sax.SAXException;

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
    Path tempFile = null;
    try {
      tempFile = downloadSource.downloadToFile(input);
      return new ExtractionResult.Extracted(input, TikaSupport.extractText(tempFile, createParseContext()));
    } catch (TikaException exception) {
      return tikaFailure(input, exception);
    } catch (IOException | SAXException exception) {
      return new ExtractionResult.Flagged(
          input, ExtractionFailureReason.EXTRACTION_ERROR, exception.getMessage());
    } finally {
      TempFileSupport.deleteTempFile(tempFile);
    }
  }

  private static ExtractionResult tikaFailure(ExtractionInput input, TikaException exception) {
    return isPasswordProtected(exception)
        ? new ExtractionResult.Flagged(
            input, ExtractionFailureReason.PASSWORD_PROTECTED, PASSWORD_PROTECTED_DETAIL)
        : new ExtractionResult.Flagged(
            input, ExtractionFailureReason.EXTRACTION_ERROR, exception.getMessage());
  }

  private static boolean isPasswordProtected(TikaException exception) {
    for (Throwable current = exception; nonNull(current); current = current.getCause()) {
      if (current instanceof InvalidPasswordException) {
        return true;
      }
    }
    return false;
  }

  private static ParseContext createParseContext() {
    var context = new ParseContext();
    var pdfConfig = new PDFParserConfig();
    pdfConfig.setSortByPosition(true);
    context.set(PDFParserConfig.class, pdfConfig);
    return context;
  }
}
