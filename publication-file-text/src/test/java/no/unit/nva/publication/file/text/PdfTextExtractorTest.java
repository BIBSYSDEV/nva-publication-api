package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDFormContentStream;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
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
  private static final int IMAGE_SIDE_PIXELS = 50;
  private static final float IMAGE_ORIGIN = 0;
  private static final float FORM_WIDTH = 300;
  private static final float FORM_HEIGHT = 100;
  private static final float FORM_TEXT_INSET = 10;
  private static final int TWO_PAGES = 2;
  private static final int THREE_IMAGES = 3;

  @TempDir Path tempDir;

  @Test
  void shouldFlagImageOnlyPdfWithScanEvidence() throws IOException {
    var scannedPdf = imageOnlyPdf(TWO_PAGES);

    var result = defaultPdfTextExtractor().extract(SOME_INPUT, scannedPdf);

    assertThat(result)
        .asInstanceOf(type(ExtractionResult.Flagged.class))
        .satisfies(
            flagged -> {
              assertThat(flagged.reason()).isEqualTo(ExtractionFailureReason.IMAGE_ONLY_CONTENT);
              assertThat(flagged.detail())
                  .contains("2 pages")
                  .contains("images on 2 pages")
                  .contains("OCR");
            });
  }

  @Test
  void shouldRouteImageOnlyPdfToInjectedProcessorAsOcrSeam() throws IOException {
    var ocrText = "text recovered by OCR";
    ImageOnlyPdfProcessor ocrProcessor =
        (input, file, fingerprint) -> new ExtractionResult.Extracted(input, ocrText, false);
    var scannedPdf = imageOnlyPdf(1);

    var result = new PdfTextExtractor(ocrProcessor).extract(SOME_INPUT, scannedPdf);

    assertThat(result).isInstanceOf(ExtractionResult.Extracted.class);
    assertThat(((ExtractionResult.Extracted) result).text()).isEqualTo(ocrText);
  }

  @Test
  void shouldExtractTextFromPdfContainingBothTextAndImages() throws IOException {
    var expectedText = "Article with figures";
    var mixedPdf = pdfWithTextAndImages(expectedText, THREE_IMAGES);

    var result = defaultPdfTextExtractor().extract(SOME_INPUT, mixedPdf);

    assertThat(result).isInstanceOf(ExtractionResult.Extracted.class);
    assertThat(((ExtractionResult.Extracted) result).text()).contains(expectedText);
  }

  @Test
  void shouldNotClassifyPdfWithoutImagesAsImageOnly() throws IOException {
    var emptyPagePdf = pdfWithSingleEmptyPage();

    var result = defaultPdfTextExtractor().extract(SOME_INPUT, emptyPagePdf);

    assertThat(result).isInstanceOf(ExtractionResult.Extracted.class);
    assertThat(((ExtractionResult.Extracted) result).text()).isBlank();
  }

  @Test
  void shouldNotClassifyPdfWithTextHiddenInFormXObjectAsImageOnly() throws IOException {
    var hiddenText = "Hidden in form";
    var pdfWithNestedText = pdfWithImageAndTextInsideFormXObject(hiddenText);

    var result = defaultPdfTextExtractor().extract(SOME_INPUT, pdfWithNestedText);

    assertThat(result).isInstanceOf(ExtractionResult.Extracted.class);
    assertThat(((ExtractionResult.Extracted) result).text()).contains(hiddenText);
  }

  @Test
  void shouldNotClassifyPdfWithInteractiveFormFieldsAsImageOnly() throws IOException {
    var pdfWithFormField = pdfWithImageAndAcroFormField();

    var result = defaultPdfTextExtractor().extract(SOME_INPUT, pdfWithFormField);

    assertThat(result).isInstanceOf(ExtractionResult.Extracted.class);
  }

  @Test
  void shouldReturnFlaggedWithParseErrorWhenFileIsCorrupted() throws IOException {
    var corruptFile = tempDir.resolve("corrupt.pdf");
    Files.writeString(corruptFile, "%PDF-1.4\nnot a valid pdf body");

    var result = defaultPdfTextExtractor().extract(SOME_INPUT, corruptFile);

    assertThat(result)
        .asInstanceOf(type(ExtractionResult.Flagged.class))
        .extracting(ExtractionResult.Flagged::reason)
        .isEqualTo(ExtractionFailureReason.PARSE_ERROR);
  }

  @Test
  void shouldReturnFlaggedWithPasswordProtectedWhenPdfIsEncrypted() throws IOException {
    var encryptedPdf = encryptedPdf();

    var result = defaultPdfTextExtractor().extract(SOME_INPUT, encryptedPdf);

    assertThat(result)
        .asInstanceOf(type(ExtractionResult.Flagged.class))
        .extracting(ExtractionResult.Flagged::reason)
        .isEqualTo(ExtractionFailureReason.PASSWORD_PROTECTED);
  }

  @Test
  void shouldExtractTextFromValidPdf() throws IOException {
    var expectedText = "Hello NVA";
    var validPdf = validPdfWithText(expectedText);

    var result = defaultPdfTextExtractor().extract(SOME_INPUT, validPdf);

    assertThat(result).isInstanceOf(ExtractionResult.Extracted.class);
    assertThat(((ExtractionResult.Extracted) result).text()).contains(expectedText);
  }

  @Test
  void shouldSupportOnlyPdfContentType() {
    var extractor = defaultPdfTextExtractor();

    assertThat(extractor.supports(PDF_CONTENT_TYPE)).isTrue();
    assertThat(extractor.supports(DOCX_CONTENT_TYPE)).isFalse();
    assertThat(extractor.supports(DOC_CONTENT_TYPE)).isFalse();
    assertThat(extractor.supports(PLAIN_TEXT_CONTENT_TYPE)).isFalse();
  }

  private static PdfTextExtractor defaultPdfTextExtractor() {
    return new PdfTextExtractor(new FlaggingImageOnlyPdfProcessor());
  }

  private Path imageOnlyPdf(int pageCount) throws IOException {
    var path = tempDir.resolve("scan.pdf");
    try (var document = new PDDocument()) {
      var image = LosslessFactory.createFromImage(document, blankBitmap());
      for (var pageNumber = 0; pageNumber < pageCount; pageNumber++) {
        var page = new PDPage();
        document.addPage(page);
        try (var contentStream = new PDPageContentStream(document, page)) {
          contentStream.drawImage(image, IMAGE_ORIGIN, IMAGE_ORIGIN);
        }
      }
      document.save(path.toFile());
    }
    return path;
  }

  private Path pdfWithTextAndImages(String text, int imageCount) throws IOException {
    var path = tempDir.resolve("mixed.pdf");
    try (var document = new PDDocument()) {
      var page = new PDPage();
      document.addPage(page);
      var image = LosslessFactory.createFromImage(document, blankBitmap());
      try (var contentStream = new PDPageContentStream(document, page)) {
        for (var imageNumber = 0; imageNumber < imageCount; imageNumber++) {
          contentStream.drawImage(image, imageNumber * IMAGE_SIDE_PIXELS, IMAGE_ORIGIN);
        }
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

  private Path pdfWithSingleEmptyPage() throws IOException {
    var path = tempDir.resolve("empty-page.pdf");
    try (var document = new PDDocument()) {
      document.addPage(new PDPage());
      document.save(path.toFile());
    }
    return path;
  }

  private Path pdfWithImageAndTextInsideFormXObject(String text) throws IOException {
    var path = tempDir.resolve("form-text.pdf");
    try (var document = new PDDocument()) {
      var page = new PDPage();
      document.addPage(page);
      var form = new PDFormXObject(document);
      form.setBBox(new PDRectangle(FORM_WIDTH, FORM_HEIGHT));
      form.setResources(new PDResources());
      try (var formStream = new PDFormContentStream(form)) {
        formStream.beginText();
        formStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE);
        formStream.newLineAtOffset(FORM_TEXT_INSET, FORM_TEXT_INSET);
        formStream.showText(text);
        formStream.endText();
      }
      var image = LosslessFactory.createFromImage(document, blankBitmap());
      try (var contentStream = new PDPageContentStream(document, page)) {
        contentStream.drawImage(image, IMAGE_ORIGIN, IMAGE_ORIGIN);
        contentStream.drawForm(form);
      }
      document.save(path.toFile());
    }
    return path;
  }

  private Path pdfWithImageAndAcroFormField() throws IOException {
    var path = tempDir.resolve("acroform.pdf");
    try (var document = new PDDocument()) {
      var page = new PDPage();
      document.addPage(page);
      var image = LosslessFactory.createFromImage(document, blankBitmap());
      try (var contentStream = new PDPageContentStream(document, page)) {
        contentStream.drawImage(image, IMAGE_ORIGIN, IMAGE_ORIGIN);
      }
      var acroForm = new PDAcroForm(document);
      document.getDocumentCatalog().setAcroForm(acroForm);
      var field = new PDTextField(acroForm);
      field.setPartialName("note");
      acroForm.getFields().add(field);
      document.save(path.toFile());
    }
    return path;
  }

  private static BufferedImage blankBitmap() {
    return new BufferedImage(IMAGE_SIDE_PIXELS, IMAGE_SIDE_PIXELS, BufferedImage.TYPE_INT_RGB);
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
