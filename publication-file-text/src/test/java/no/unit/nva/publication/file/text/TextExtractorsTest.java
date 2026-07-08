package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TextExtractorsTest {

  private static final String DOCX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final FileDownloadSource UNUSED_DOWNLOAD_SOURCE = ignored -> Path.of("/unused");

  @Test
  void shouldProvideExtractorsInDispatchOrder() {
    var extractors = TextExtractors.defaultExtractors(UNUSED_DOWNLOAD_SOURCE);

    assertThat(extractors)
        .hasExactlyElementsOfTypes(
            PdfTextExtractor.class, WordTextExtractor.class, LatexTextExtractor.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "application/pdf",
        DOCX_CONTENT_TYPE,
        "application/msword",
        "application/x-latex",
        "application/x-tex",
        "text/x-tex"
      })
  void shouldSupportEachDefaultContentTypeWithExactlyOneExtractor(String contentType) {
    var extractors = TextExtractors.defaultExtractors(UNUSED_DOWNLOAD_SOURCE);

    var supportingExtractors =
        extractors.stream().filter(extractor -> extractor.supports(contentType)).toList();

    assertThat(supportingExtractors).hasSize(1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"text/plain", "image/jpeg", "application/octet-stream", ""})
  void shouldSupportNoUnhandledContentType(String contentType) {
    var extractors = TextExtractors.defaultExtractors(UNUSED_DOWNLOAD_SOURCE);

    assertThat(extractors).noneMatch(extractor -> extractor.supports(contentType));
  }
}
