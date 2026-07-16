package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExceptionCausesTest {

  @Test
  void shouldReturnTrueWhenExceptionIsDirectlyOfMatchingType() {
    assertThat(
            ExceptionCauses.hasCauseOfType(
                new IllegalArgumentException(), IllegalArgumentException.class))
        .isTrue();
  }

  @Test
  void shouldReturnTrueWhenMatchingTypeIsInCauseChain() {
    var root = new IllegalStateException("root");
    var wrapper = new RuntimeException("wrapper", root);

    assertThat(ExceptionCauses.hasCauseOfType(wrapper, IllegalStateException.class)).isTrue();
  }

  @Test
  void shouldReturnFalseWhenNoMatchInCauseChain() {
    var exception = new RuntimeException("no match", new IllegalArgumentException("cause"));

    assertThat(ExceptionCauses.hasCauseOfType(exception, IllegalStateException.class)).isFalse();
  }

  @Test
  void shouldMatchAnyOfMultipleTypes() {
    var exception = new IllegalArgumentException("test");

    assertThat(
            ExceptionCauses.hasCauseOfType(
                exception, IllegalStateException.class, IllegalArgumentException.class))
        .isTrue();
  }
}
