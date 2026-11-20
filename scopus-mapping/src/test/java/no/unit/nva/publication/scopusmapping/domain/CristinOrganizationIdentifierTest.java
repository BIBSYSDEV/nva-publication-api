package no.unit.nva.publication.scopusmapping.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CristinOrganizationIdentifierTest {

  @ParameterizedTest
  @ValueSource(strings = {"194.0.0.0", "194.63.10.0", "20754.6.0.0"})
  void shouldKeepFourNumbersAsValue(String value) {
    assertThat(new CristinOrganizationIdentifier(value).value()).isEqualTo(value);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "194",
        "194.0.0",
        "194.0.0.0.0",
        "194.a.0.0",
        "https://api.nva.unit.no/cristin/organization/194.0.0.0",
        ""
      })
  void shouldRejectValueThatIsNotFourNumbers(String value) {
    assertThatThrownBy(() -> new CristinOrganizationIdentifier(value))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(value);
  }

  @Test
  void shouldRejectNull() {
    assertThatNullPointerException().isThrownBy(() -> new CristinOrganizationIdentifier(null));
  }

  @ParameterizedTest
  @CsvSource({"194.0.0.0, 194.0.0.0", "194.63.10.0, 194.0.0.0", "20754.6.0.0, 20754.0.0.0"})
  void shouldFindTopLevelOfOrganization(String organization, String expectedTopLevel) {
    assertThat(new CristinOrganizationIdentifier(organization).topLevel())
        .isEqualTo(new CristinOrganizationIdentifier(expectedTopLevel));
  }

  @ParameterizedTest
  @CsvSource({"194.0.0.0, true", "194.63.10.0, false", "194.0.0.1, false"})
  void shouldKnowWhetherItIsTopLevel(String organization, boolean expectedToBeTopLevel) {
    assertThat(new CristinOrganizationIdentifier(organization).isTopLevel())
        .isEqualTo(expectedToBeTopLevel);
  }
}
