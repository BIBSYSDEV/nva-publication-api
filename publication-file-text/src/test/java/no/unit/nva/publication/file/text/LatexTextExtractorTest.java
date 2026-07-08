package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LatexTextExtractorTest {

  private static final String TEX_CONTENT_TYPE = "application/x-tex";
  private static final String LATEX_CONTENT_TYPE = "application/x-latex";
  private static final String PDF_CONTENT_TYPE = "application/pdf";
  private static final String PLAIN_TEXT_CONTENT_TYPE = "text/plain";
  private static final ExtractionInput SOME_INPUT =
      new ExtractionInput("bucket", "key.tex", "etag", TEX_CONTENT_TYPE);

  @TempDir Path tempDir;

  @Test
  void shouldExtractTextFromValidLatexDocument() throws IOException {
    var expectedText = "Hello NVA";
    var latexFile = tempDir.resolve("document.tex");
    Files.writeString(latexFile, "\\begin{document}\n" + expectedText + "\n\\end{document}");

    var result = new LatexTextExtractor().extract(SOME_INPUT, latexFile);

    assertThat(result).isInstanceOf(ExtractionResult.Extracted.class);
    assertThat(((ExtractionResult.Extracted) result).text()).contains(expectedText);
  }

  @Test
  void shouldSupportTexAndLatexContentTypes() {
    var extractor = new LatexTextExtractor();

    assertThat(extractor.supports(TEX_CONTENT_TYPE)).isTrue();
    assertThat(extractor.supports(LATEX_CONTENT_TYPE)).isTrue();
    assertThat(extractor.supports(PDF_CONTENT_TYPE)).isFalse();
    assertThat(extractor.supports(PLAIN_TEXT_CONTENT_TYPE)).isFalse();
  }
}
