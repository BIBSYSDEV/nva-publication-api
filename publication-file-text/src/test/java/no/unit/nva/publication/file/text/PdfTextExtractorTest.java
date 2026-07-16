package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfTextExtractorTest {

  private static final String PDF_CONTENT_TYPE = "application/pdf";
  private static final String DOCX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final String DOC_CONTENT_TYPE = "application/msword";
  private static final String PLAIN_TEXT_CONTENT_TYPE = "text/plain";
  private static final ExtractionInput SOME_INPUT =
      new ExtractionInput("bucket", "key.pdf", "etag", PDF_CONTENT_TYPE);
  private static final float FONT_SIZE = 12;
  private static final float TEXT_X_OFFSET = 100;
  private static final float TEXT_Y_OFFSET = 700;
  private static final int ENCRYPTION_KEY_LENGTH_BITS = 256;

  @TempDir Path tempDir;

  @Test
  void shouldReturnFlaggedWithParseErrorWhenFileIsCorrupted() throws IOException {
    var corruptFile = tempDir.resolve("corrupt.pdf");
    Files.writeString(corruptFile, "%PDF-1.4\nnot a valid pdf body");

    var result = new PdfTextExtractor().extract(SOME_INPUT, corruptFile);

    assertThat(result)
        .asInstanceOf(type(ExtractionResult.Flagged.class))
        .extracting(ExtractionResult.Flagged::reason)
        .isEqualTo(ExtractionFailureReason.PARSE_ERROR);
  }

  @Test
  void shouldReturnFlaggedWithPasswordProtectedWhenPdfIsEncrypted() throws IOException {
    var encryptedPdf = encryptedPdf();

    var result = new PdfTextExtractor().extract(SOME_INPUT, encryptedPdf);

    assertThat(result)
        .asInstanceOf(type(ExtractionResult.Flagged.class))
        .extracting(ExtractionResult.Flagged::reason)
        .isEqualTo(ExtractionFailureReason.PASSWORD_PROTECTED);
  }

  @Test
  void shouldExtractTextFromValidPdf() throws IOException {
    var expectedText = "Hello NVA";
    var validPdf = validPdfWithText(expectedText);

    var result = new PdfTextExtractor().extract(SOME_INPUT, validPdf);

    assertThat(result).isInstanceOf(ExtractionResult.Extracted.class);
    assertThat(((ExtractionResult.Extracted) result).text()).contains(expectedText);
  }

  @Test
  void shouldSupportOnlyPdfContentType() {
    var extractor = new PdfTextExtractor();

    assertThat(extractor.supports(PDF_CONTENT_TYPE)).isTrue();
    assertThat(extractor.supports(DOCX_CONTENT_TYPE)).isFalse();
    assertThat(extractor.supports(DOC_CONTENT_TYPE)).isFalse();
    assertThat(extractor.supports(PLAIN_TEXT_CONTENT_TYPE)).isFalse();
  }

  private Path validPdfWithText(String text) throws IOException {
    var path = tempDir.resolve("valid.pdf");
    try (var document = new PDDocument()) {
      var page = new PDPage();
      document.addPage(page);
      try (var contentStream = new PDPageContentStream(document, page)) {
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE);
        contentStream.newLineAtOffset(TEXT_X_OFFSET, TEXT_Y_OFFSET);
        contentStream.showText(text);
        contentStream.endText();
      }
      document.save(path.toFile());
    }
    return path;
  }

  private Path encryptedPdf() throws IOException {
    var path = tempDir.resolve("encrypted.pdf");
    try (var document = new PDDocument()) {
      document.addPage(new PDPage());
      var policy =
          new StandardProtectionPolicy("owner-secret", "user-secret", new AccessPermission());
      policy.setEncryptionKeyLength(ENCRYPTION_KEY_LENGTH_BITS);
      document.protect(policy);
      document.save(path.toFile());
    }
    return path;
  }
}
