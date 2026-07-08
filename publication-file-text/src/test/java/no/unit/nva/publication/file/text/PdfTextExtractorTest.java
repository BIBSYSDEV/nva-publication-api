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

  private static final ExtractionInput SOME_INPUT =
      new ExtractionInput("bucket", "key.pdf", "etag", "application/pdf");
  private static final float FONT_SIZE = 12;
  private static final float TEXT_X_OFFSET = 100;
  private static final float TEXT_Y_OFFSET = 700;
  private static final int ENCRYPTION_KEY_LENGTH_BITS = 256;

  @TempDir Path tempDir;

  @Test
  void shouldReturnFlaggedWithExtractionErrorWhenDownloadFails() {
    FileDownloadSource failingSource =
        input -> {
          throw new IOException("S3 download failed");
        };
    var extractor = new PdfTextExtractor(failingSource);

    var result = extractor.extract(SOME_INPUT);

    assertThat(result)
        .isEqualTo(
            new ExtractionResult.Flagged(
                SOME_INPUT, ExtractionFailureReason.EXTRACTION_ERROR, "S3 download failed"));
  }

  @Test
  void shouldReturnFlaggedWithExtractionErrorWhenFileIsCorrupted() throws IOException {
    var corruptFile = tempDir.resolve("corrupt.pdf");
    Files.writeString(corruptFile, "%PDF-1.4\nnot a valid pdf body");
    var extractor = new PdfTextExtractor(input -> corruptFile);

    var result = extractor.extract(SOME_INPUT);

    assertThat(result)
        .asInstanceOf(type(ExtractionResult.Flagged.class))
        .extracting(ExtractionResult.Flagged::reason)
        .isEqualTo(ExtractionFailureReason.EXTRACTION_ERROR);
  }

  @Test
  void shouldReturnFlaggedWithPasswordProtectedWhenPdfIsEncrypted() throws IOException {
    var encryptedPdf = encryptedPdf();
    var extractor = new PdfTextExtractor(input -> encryptedPdf);

    var result = extractor.extract(SOME_INPUT);

    assertThat(result)
        .asInstanceOf(type(ExtractionResult.Flagged.class))
        .extracting(ExtractionResult.Flagged::reason)
        .isEqualTo(ExtractionFailureReason.PASSWORD_PROTECTED);
  }

  @Test
  void shouldExtractTextFromValidPdf() throws IOException {
    var expectedText = "Hello NVA";
    var validPdf = validPdfWithText(expectedText);
    var extractor = new PdfTextExtractor(input -> validPdf);

    var result = extractor.extract(SOME_INPUT);

    assertThat(result).isInstanceOf(ExtractionResult.Extracted.class);
    assertThat(((ExtractionResult.Extracted) result).text()).contains(expectedText);
  }

  @Test
  void shouldSupportOnlyPdfContentType() {
    var extractor = new PdfTextExtractor(input -> Path.of("/unused"));

    assertThat(extractor.supports("application/pdf")).isTrue();
    assertThat(
            extractor.supports(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .isFalse();
    assertThat(extractor.supports("application/msword")).isFalse();
    assertThat(extractor.supports("text/plain")).isFalse();
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
