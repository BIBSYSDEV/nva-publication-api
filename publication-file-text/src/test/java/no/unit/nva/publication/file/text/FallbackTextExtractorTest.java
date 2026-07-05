package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FallbackTextExtractorTest {

  private static final String APPLICATION_PDF = "application/pdf";
  private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final String IMAGE_JPG = "image/jpeg";
  private static final String EMPTY_STRING = "";
  private final FallbackTextExtractor extractor = new FallbackTextExtractor();

  @ParameterizedTest
  @ValueSource(strings = { APPLICATION_PDF, DOCX, IMAGE_JPG,  EMPTY_STRING })
  void shouldSupportAnyContentType(String contentType) {
    assertThat(extractor.supports(contentType)).isTrue();
  }

  @Test
  void shouldReturnFlaggedResultWithUnsupportedFormatReason() {
    var input = new ExtractionInput("bucket", "key", "etag", APPLICATION_PDF);

    var result = extractor.extract(input);

    assertThat(result).isInstanceOf(ExtractionResult.Flagged.class);
    var flagged = (ExtractionResult.Flagged) result;
    assertThat(flagged.reason()).isEqualTo(ExtractionFailureReason.UNSUPPORTED_FORMAT);
    assertThat(flagged.source()).isEqualTo(input);
    assertThat(flagged.detail()).isEqualTo(APPLICATION_PDF);
  }
}
