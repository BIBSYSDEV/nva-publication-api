package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocxTextExtractorTest {

  private static final String DOCX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final ExtractionInput SOME_INPUT =
      new ExtractionInput("bucket", "key.docx", "etag", DOCX_CONTENT_TYPE);

  @TempDir Path tempDir;

  @Test
  void shouldReturnFlaggedWithExtractionErrorWhenDownloadFails() {
    FileDownloadSource failingSource =
        input -> {
          throw new IOException("S3 download failed");
        };
    var extractor = new DocxTextExtractor(failingSource);

    var result = extractor.extract(SOME_INPUT);

    assertThat(result)
        .isEqualTo(
            new ExtractionResult.Flagged(
                SOME_INPUT, ExtractionFailureReason.EXTRACTION_ERROR, "S3 download failed"));
  }

  @Test
  void shouldReturnFlaggedWithExtractionErrorWhenFileIsCorrupted() throws IOException {
    var corruptFile = tempDir.resolve("corrupt.docx");
    Files.write(
        corruptFile,
        new byte[] {
          (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
        });
    var extractor = new DocxTextExtractor(input -> corruptFile);

    var result = extractor.extract(SOME_INPUT);

    assertThat(result)
        .asInstanceOf(type(ExtractionResult.Flagged.class))
        .extracting(ExtractionResult.Flagged::reason)
        .isEqualTo(ExtractionFailureReason.EXTRACTION_ERROR);
  }

  @Test
  void shouldReturnFlaggedWithPasswordProtectedWhenDocxIsEncrypted()
      throws IOException, GeneralSecurityException {
    var encryptedDocx = encryptedDocx();
    var extractor = new DocxTextExtractor(input -> encryptedDocx);

    var result = extractor.extract(SOME_INPUT);

    assertThat(result)
        .asInstanceOf(type(ExtractionResult.Flagged.class))
        .extracting(ExtractionResult.Flagged::reason)
        .isEqualTo(ExtractionFailureReason.PASSWORD_PROTECTED);
  }

  @Test
  void shouldExtractTextFromValidDocx() throws IOException {
    var expectedText = "Hello NVA";
    var validDocx = validDocxWithText(expectedText);
    var extractor = new DocxTextExtractor(input -> validDocx);

    var result = extractor.extract(SOME_INPUT);

    assertThat(result).isInstanceOf(ExtractionResult.Extracted.class);
    assertThat(((ExtractionResult.Extracted) result).text()).contains(expectedText);
  }

  @Test
  void shouldSupportOnlyDocxContentType() {
    var extractor = new DocxTextExtractor(input -> Path.of("/unused"));

    assertThat(extractor.supports(DOCX_CONTENT_TYPE)).isTrue();
    assertThat(extractor.supports("application/pdf")).isFalse();
    assertThat(extractor.supports("application/msword")).isFalse();
    assertThat(extractor.supports("text/plain")).isFalse();
  }

  private Path validDocxWithText(String text) throws IOException {
    var path = tempDir.resolve("valid.docx");
    try (var document = new XWPFDocument()) {
      document.createParagraph().createRun().setText(text);
      try (var out = Files.newOutputStream(path)) {
        document.write(out);
      }
    }
    return path;
  }

  private Path encryptedDocx() throws IOException, GeneralSecurityException {
    var path = tempDir.resolve("encrypted.docx");
    try (var document = new XWPFDocument()) {
      document.createParagraph().createRun().setText("secret");
      var poifs = new POIFSFileSystem();
      var encryptionInfo = new EncryptionInfo(EncryptionMode.agile);
      var encryptor = encryptionInfo.getEncryptor();
      encryptor.confirmPassword("password");
      try (var encryptedStream = encryptor.getDataStream(poifs)) {
        document.write(encryptedStream);
      }
      try (var out = Files.newOutputStream(path)) {
        poifs.writeFilesystem(out);
      }
      poifs.close();
    }
    return path;
  }
}
