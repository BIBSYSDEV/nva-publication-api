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

class WordTextExtractorTest {

  private static final String DOCX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final String DOC_CONTENT_TYPE = "application/msword";
  private static final String PDF_CONTENT_TYPE = "application/pdf";
  private static final String PLAIN_TEXT_CONTENT_TYPE = "text/plain";

  @TempDir Path tempDir;

  @Test
  void shouldReturnFlaggedWithParseErrorWhenDocxIsCorrupted() throws IOException {
    var corruptFile = tempDir.resolve("corrupt.docx");
    Files.write(corruptFile, ole2MagicBytesOnly());

    var result = new WordTextExtractor().extract(inputFor(DOCX_CONTENT_TYPE), corruptFile);

    assertThat(result)
        .asInstanceOf(type(ExtractionResult.Flagged.class))
        .extracting(ExtractionResult.Flagged::reason)
        .isEqualTo(ExtractionFailureReason.PARSE_ERROR);
  }

  @Test
  void shouldReturnFlaggedWithParseErrorWhenDocIsCorrupted() throws IOException {
    var corruptFile = tempDir.resolve("corrupt.doc");
    Files.write(corruptFile, ole2MagicBytesOnly());

    var result = new WordTextExtractor().extract(inputFor(DOC_CONTENT_TYPE), corruptFile);

    assertThat(result)
        .asInstanceOf(type(ExtractionResult.Flagged.class))
        .extracting(ExtractionResult.Flagged::reason)
        .isEqualTo(ExtractionFailureReason.PARSE_ERROR);
  }

  @Test
  void shouldReturnFlaggedWithPasswordProtectedWhenDocxIsEncrypted()
      throws IOException, GeneralSecurityException {
    var encryptedDocx = encryptedDocx();

    var result = new WordTextExtractor().extract(inputFor(DOCX_CONTENT_TYPE), encryptedDocx);

    assertThat(result)
        .asInstanceOf(type(ExtractionResult.Flagged.class))
        .extracting(ExtractionResult.Flagged::reason)
        .isEqualTo(ExtractionFailureReason.PASSWORD_PROTECTED);
  }

  @Test
  void shouldReturnFlaggedWithPasswordProtectedWhenDocIsEncrypted()
      throws IOException, GeneralSecurityException {
    var encryptedDoc = encryptedDoc();

    var result = new WordTextExtractor().extract(inputFor(DOC_CONTENT_TYPE), encryptedDoc);

    assertThat(result)
        .asInstanceOf(type(ExtractionResult.Flagged.class))
        .extracting(ExtractionResult.Flagged::reason)
        .isEqualTo(ExtractionFailureReason.PASSWORD_PROTECTED);
  }

  @Test
  void shouldExtractTextFromValidDocx() throws IOException {
    var expectedText = "Hello NVA";
    var validDocx = validDocxWithText(expectedText);

    var result = new WordTextExtractor().extract(inputFor(DOCX_CONTENT_TYPE), validDocx);

    assertThat(result).isInstanceOf(ExtractionResult.Extracted.class);
    assertThat(((ExtractionResult.Extracted) result).text()).contains(expectedText);
  }

  @Test
  void shouldSupportBothWordContentTypes() {
    var extractor = new WordTextExtractor();

    assertThat(extractor.supports(DOCX_CONTENT_TYPE)).isTrue();
    assertThat(extractor.supports(DOC_CONTENT_TYPE)).isTrue();
  }

  @Test
  void shouldNotSupportOtherContentTypes() {
    var extractor = new WordTextExtractor();

    assertThat(extractor.supports(PDF_CONTENT_TYPE)).isFalse();
    assertThat(extractor.supports(PLAIN_TEXT_CONTENT_TYPE)).isFalse();
  }

  private ExtractionInput inputFor(String contentType) {
    var extension = DOCX_CONTENT_TYPE.equals(contentType) ? "docx" : "doc";
    return new ExtractionInput("bucket", "key." + extension, "etag", contentType);
  }

  private static byte[] ole2MagicBytesOnly() {
    return new byte[] {
      (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
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

  private Path encryptedDoc() throws IOException, GeneralSecurityException {
    var path = tempDir.resolve("encrypted.doc");
    var poifs = new POIFSFileSystem();
    var encryptionInfo = new EncryptionInfo(EncryptionMode.agile);
    var encryptor = encryptionInfo.getEncryptor();
    encryptor.confirmPassword("password");
    try (var encryptedStream = encryptor.getDataStream(poifs)) {
      encryptedStream.write(new byte[] {1, 2, 3, 4});
    }
    try (var out = Files.newOutputStream(path)) {
      poifs.writeFilesystem(out);
    }
    poifs.close();
    return path;
  }
}
