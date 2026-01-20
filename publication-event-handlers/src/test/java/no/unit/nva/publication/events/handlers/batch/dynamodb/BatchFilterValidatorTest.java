package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class BatchFilterValidatorTest {

  @Test
  void shouldAcceptNullFilter() {
    assertDoesNotThrow(() -> BatchFilterValidator.validate(null));
  }

  @Test
  void shouldAcceptNullImportSources() {
    var filter = new BatchFilter(null, null, null, null);
    assertDoesNotThrow(() -> BatchFilterValidator.validate(filter));
  }

  @Test
  void shouldAcceptEmptyImportSources() {
    var filter = new BatchFilter(null, null, List.of(), null);
    assertDoesNotThrow(() -> BatchFilterValidator.validate(filter));
  }

  @Test
  void shouldAcceptValidImportSource() {
    var filter = new BatchFilter(null, null, List.of("SCOPUS"), null);
    assertDoesNotThrow(() -> BatchFilterValidator.validate(filter));
  }

  @Test
  void shouldAcceptMultipleValidImportSources() {
    var filter = new BatchFilter(null, null, List.of("SCOPUS", "CRISTIN"), null);
    assertDoesNotThrow(() -> BatchFilterValidator.validate(filter));
  }

  @Test
  void shouldThrowOnInvalidImportSource() {
    var filter = new BatchFilter(null, null, List.of("INVALID_SOURCE"), null);
    var exception =
        assertThrows(IllegalArgumentException.class, () -> BatchFilterValidator.validate(filter));
    assertThat(exception.getMessage(), containsString("INVALID_SOURCE"));
  }

  @Test
  void shouldThrowOnMixedValidAndInvalidImportSources() {
    var filter = new BatchFilter(null, null, List.of("SCOPUS", "NOT_A_SOURCE"), null);
    var exception =
        assertThrows(IllegalArgumentException.class, () -> BatchFilterValidator.validate(filter));
    assertThat(exception.getMessage(), containsString("NOT_A_SOURCE"));
  }
}
