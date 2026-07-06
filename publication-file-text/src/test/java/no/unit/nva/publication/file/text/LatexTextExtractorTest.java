package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LatexTextExtractorTest {

  private static final ExtractionInput SOME_INPUT =
      new ExtractionInput("bucket", "key.tex", "etag", "application/x-latex");

  @TempDir Path tempDir;

  @Test
  void shouldReturnFlaggedWithExtractionErrorWhenDownloadFails() {
    FileDownloadSource failingSource =
        ignored -> {
          throw new IOException("S3 download failed");
        };

    var result = new LatexTextExtractor(failingSource).extract(SOME_INPUT);

    assertThat(result)
        .isEqualTo(
            new ExtractionResult.Flagged(
                SOME_INPUT, ExtractionFailureReason.EXTRACTION_ERROR, "S3 download failed"));
  }

  @Test
  void shouldExtractTextFromValidLatexDocument() throws IOException {
    var expectedText = "Hello NVA";
    var latexFile = tempDir.resolve("document.tex");
    Files.writeString(latexFile, "\\begin{document}\n" + expectedText + "\n\\end{document}");

    var result = new LatexTextExtractor(ignored -> latexFile).extract(SOME_INPUT);

    assertThat(result).isInstanceOf(ExtractionResult.Extracted.class);
    assertThat(((ExtractionResult.Extracted) result).text()).contains(expectedText);
  }

  @Test
  void shouldSupportLatexContentType() {
    var extractor = new LatexTextExtractor(ignored -> Path.of("/unused"));

    assertThat(extractor.supports("application/x-latex")).isTrue();
    assertThat(extractor.supports("application/pdf")).isFalse();
    assertThat(extractor.supports("text/plain")).isFalse();
  }
}
