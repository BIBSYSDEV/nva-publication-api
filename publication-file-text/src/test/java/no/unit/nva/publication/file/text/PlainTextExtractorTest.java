package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlainTextExtractorTest {

  private static final String PLAIN_TEXT_CONTENT_TYPE = "text/plain";
  private static final String PDF_CONTENT_TYPE = "application/pdf";
  private static final String TEX_CONTENT_TYPE = "application/x-tex";
  private static final ExtractionInput SOME_INPUT =
      new ExtractionInput("bucket", "key.txt", "etag", PLAIN_TEXT_CONTENT_TYPE);

  @TempDir Path tempDir;

  @Test
  void shouldExtractTextFromPlainTextFile() throws IOException {
    var expectedText = "Hello NVA plain text";
    var textFile = tempDir.resolve("document.txt");
    Files.writeString(textFile, expectedText);

    var result = new PlainTextExtractor().extract(SOME_INPUT, textFile);

    assertThat(result).isInstanceOf(ExtractionResult.Extracted.class);
    assertThat(((ExtractionResult.Extracted) result).text()).contains(expectedText);
  }

  @Test
  void shouldSupportOnlyPlainTextContentType() {
    var extractor = new PlainTextExtractor();

    assertThat(extractor.supports(PLAIN_TEXT_CONTENT_TYPE)).isTrue();
    assertThat(extractor.supports(PDF_CONTENT_TYPE)).isFalse();
    assertThat(extractor.supports(TEX_CONTENT_TYPE)).isFalse();
  }
}
