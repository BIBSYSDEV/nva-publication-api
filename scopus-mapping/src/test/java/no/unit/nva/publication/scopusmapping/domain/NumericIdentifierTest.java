package no.unit.nva.publication.scopusmapping.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NumericIdentifierTest {

  private static final String PLAIN_NUMBER = "99999990501";

  @ParameterizedTest
  @MethodSource("numericIdentifierTypes")
  void shouldKeepPlainNumberAsValue(Function<String, NumericIdentifier> newIdentifier) {
    assertThat(newIdentifier.apply(PLAIN_NUMBER).value()).isEqualTo(PLAIN_NUMBER);
  }

  @ParameterizedTest
  @MethodSource("numericIdentifierTypesAndValuesThatAreNotPlainNumbers")
  void shouldRejectValueThatIsNotPlainNumber(
      Function<String, NumericIdentifier> newIdentifier, String value) {
    assertThatThrownBy(() -> newIdentifier.apply(value))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be a plain number");
  }

  @ParameterizedTest
  @MethodSource("numericIdentifierTypes")
  void shouldRejectNull(Function<String, NumericIdentifier> newIdentifier) {
    assertThatNullPointerException().isThrownBy(() -> newIdentifier.apply(null));
  }

  private static Stream<Named<Function<String, NumericIdentifier>>> numericIdentifierTypes() {
    return Stream.of(
        Named.of("ScopusAuthorIdentifier", ScopusAuthorIdentifier::new),
        Named.of("ScopusAffiliationIdentifier", ScopusAffiliationIdentifier::new),
        Named.of("CristinPersonIdentifier", CristinPersonIdentifier::new));
  }

  private static Stream<Arguments> numericIdentifierTypesAndValuesThatAreNotPlainNumbers() {
    var valuesThatAreNotPlainNumbers =
        Stream.of(
                "https://api.nva.unit.no/cristin/person/12345",
                "SCOPUS:99999990501",
                "12345a",
                " 12345",
                "-12345",
                "194.0.0.0",
                "")
            .toList();
    return numericIdentifierTypes()
        .flatMap(
            identifierType ->
                valuesThatAreNotPlainNumbers.stream()
                    .map(value -> Arguments.of(identifierType, value)));
  }
}
