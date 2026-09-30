package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.ParseContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;

class TikaSupportTest {

  private static final int TINY_CHARACTER_LIMIT = 5;
  private static final String LONG_CONTENT = "abcdefghijklmnopqrstuvwxyz";
  private static final String PDF_CONTENT_TYPE = "application/pdf";
  private static final String DOCX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final String TEX_CONTENT_TYPE = "application/x-tex";
  private static final String PLAIN_TEXT_CONTENT_TYPE = "text/plain";
  private static final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";

  @TempDir Path tempDir;

  @Test
  void shouldTruncateExtractedTextWhenContentExceedsCharacterLimit()
      throws TikaException, IOException, SAXException {
    var file = tempDir.resolve("large.txt");
    Files.writeString(file, LONG_CONTENT);

    var extractedText = TikaSupport.extractText(file, new ParseContext(), TINY_CHARACTER_LIMIT);

    assertThat(extractedText.truncated()).isTrue();
    assertThat(extractedText.text()).hasSize(TINY_CHARACTER_LIMIT);
    assertThat(LONG_CONTENT).contains(extractedText.text().strip());
  }

  @Test
  void shouldReturnFullTextWithoutTruncationWhenContentIsWithinCharacterLimit()
      throws TikaException, IOException, SAXException {
    var file = tempDir.resolve("small.txt");
    Files.writeString(file, LONG_CONTENT);

    var extractedText = TikaSupport.extractText(file, new ParseContext());

    assertThat(extractedText.truncated()).isFalse();
    assertThat(extractedText.text()).contains(LONG_CONTENT);
  }

  @Test
  void shouldDetectPdfFromMagicBytes() throws IOException {
    var file = tempDir.resolve("blob");
    Files.writeString(file, "%PDF-1.4\nfake pdf body");

    var contentType = TikaSupport.detectContentType(file, "blob");

    assertThat(contentType).isEqualTo(PDF_CONTENT_TYPE);
  }

  @Test
  void shouldDetectDocxFromContainerBytes() throws IOException {
    var file = tempDir.resolve("blob.bin");
    try (var document = new XWPFDocument();
        var out = Files.newOutputStream(file)) {
      document.createParagraph().createRun().setText("Hello NVA");
      document.write(out);
    }

    var contentType = TikaSupport.detectContentType(file, "blob.bin");

    assertThat(contentType).isEqualTo(DOCX_CONTENT_TYPE);
  }

  @Test
  void shouldDetectTexFromFilenameHint() throws IOException {
    var file = tempDir.resolve("blob");
    Files.writeString(file, "% a comment first\n\\documentclass{article}\n\\begin{document}");

    var contentType = TikaSupport.detectContentType(file, "paper.tex");

    assertThat(contentType).isEqualTo(TEX_CONTENT_TYPE);
  }

  @Test
  void shouldDetectPlainTextForExtensionlessProse() throws IOException {
    var file = tempDir.resolve("blob");
    Files.writeString(file, "Just some ordinary prose without any markers.");

    var contentType = TikaSupport.detectContentType(file, "blob");

    assertThat(contentType).isEqualTo(PLAIN_TEXT_CONTENT_TYPE);
  }

  @Test
  void shouldDetectOctetStreamForUnknownBinaryContent() throws IOException {
    var file = tempDir.resolve("blob");
    Files.write(file, new byte[] {0x00, 0x01, 0x02, 0x03, (byte) 0xFF, (byte) 0xFE, 0x10, 0x00});

    var contentType = TikaSupport.detectContentType(file, "blob");

    assertThat(contentType).isEqualTo(OCTET_STREAM_CONTENT_TYPE);
  }
}
