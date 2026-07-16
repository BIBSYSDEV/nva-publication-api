package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TextExtractorsTest {

  private static final String DOCX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

  @Test
  void shouldProvideExtractorsInDispatchOrder() {
    var extractors = TextExtractors.defaultExtractors();

    assertThat(extractors)
        .hasExactlyElementsOfTypes(
            PdfTextExtractor.class,
            WordTextExtractor.class,
            LatexTextExtractor.class,
            PlainTextExtractor.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "application/pdf",
        DOCX_CONTENT_TYPE,
        "application/msword",
        "application/x-tex",
        "application/x-latex",
        "text/plain"
      })
  void shouldSupportEachDefaultContentTypeWithExactlyOneExtractor(String contentType) {
    var extractors = TextExtractors.defaultExtractors();

    var supportingExtractors =
        extractors.stream().filter(extractor -> extractor.supports(contentType)).toList();

    assertThat(supportingExtractors).hasSize(1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"image/jpeg", "application/octet-stream", "application/zip"})
  void shouldSupportNoUnhandledContentType(String contentType) {
    var extractors = TextExtractors.defaultExtractors();

    assertThat(extractors).noneMatch(extractor -> extractor.supports(contentType));
  }
}
