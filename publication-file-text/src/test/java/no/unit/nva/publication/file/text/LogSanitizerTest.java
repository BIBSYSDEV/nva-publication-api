package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LogSanitizerTest {

  private static final int MAX_SANITIZED_LENGTH = 256;
  private static final int OVERLONG_VALUE_LENGTH = 300;

  @Test
  void shouldReturnEmptyStringForNull() {
    assertThat(LogSanitizer.sanitize(null)).isEmpty();
  }

  @Test
  void shouldPreserveNormalStrings() {
    assertThat(LogSanitizer.sanitize("publications/2024/doc.pdf"))
        .isEqualTo("publications/2024/doc.pdf");
  }

  @ParameterizedTest
  @ValueSource(strings = {"\n", "\r", "\t"})
  void shouldReplaceControlCharactersWithUnderscore(String controlChar) {
    assertThat(LogSanitizer.sanitize("a" + controlChar + "b")).isEqualTo("a_b");
  }

  @Test
  void shouldReplaceAllControlCharactersInCrLf() {
    assertThat(LogSanitizer.sanitize("a\r\nb")).isEqualTo("a__b");
  }

  @Test
  void shouldTruncateLongValues() {
    var longValue = "a".repeat(OVERLONG_VALUE_LENGTH);

    var sanitized = LogSanitizer.sanitize(longValue);

    assertThat(sanitized).hasSize(MAX_SANITIZED_LENGTH);
  }
}
