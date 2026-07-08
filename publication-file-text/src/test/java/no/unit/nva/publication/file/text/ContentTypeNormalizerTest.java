package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ContentTypeNormalizerTest {

  @Test
  void shouldReturnEmptyStringWhenContentTypeIsNull() {
    var normalized = ContentTypeNormalizer.normalize(null);

    assertThat(normalized).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "application/pdf, application/pdf",
    "Application/PDF, application/pdf",
    "APPLICATION/PDF, application/pdf",
    "'application/pdf; charset=UTF-8', application/pdf",
    "'Application/PDF; charset=UTF-8; boundary=x', application/pdf",
    "' application/pdf ', application/pdf",
    "'application/x-latex;version=1', application/x-latex"
  })
  void shouldNormalizeToLowercaseParameterFreeMediaType(String raw, String expected) {
    var normalized = ContentTypeNormalizer.normalize(raw);

    assertThat(normalized).isEqualTo(expected);
  }

  @Test
  void shouldReturnEmptyStringWhenContentTypeIsBlank() {
    var normalized = ContentTypeNormalizer.normalize("   ");

    assertThat(normalized).isEmpty();
  }
}
