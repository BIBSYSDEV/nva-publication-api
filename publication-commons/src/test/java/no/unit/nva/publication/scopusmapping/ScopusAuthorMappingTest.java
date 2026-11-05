package no.unit.nva.publication.scopusmapping;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ScopusAuthorMappingTest {

  private static final String SCOPUS_AUTHOR_ID = "99999990501";
  private static final String CRISTIN_PERSON_ID = "12345";

  @Test
  void shouldAcceptNumericIds() {
    assertThatCode(() -> authorMapping(SCOPUS_AUTHOR_ID, CRISTIN_PERSON_ID))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"https://api.nva.unit.no/cristin/person/12345", "12345a", "", " 12345", "-12345"})
  void shouldRejectCristinPersonIdThatIsNotNumeric(String cristinPersonId) {
    assertThatThrownBy(() -> authorMapping(SCOPUS_AUTHOR_ID, cristinPersonId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cristinPersonId");
  }

  @ParameterizedTest
  @ValueSource(strings = {"SCOPUS:99999990501", "9999999O501", ""})
  void shouldRejectScopusAuthorIdThatIsNotNumeric(String scopusAuthorId) {
    assertThatThrownBy(() -> authorMapping(scopusAuthorId, CRISTIN_PERSON_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("scopusAuthorId");
  }

  private static ScopusAuthorMapping authorMapping(String scopusAuthorId, String cristinPersonId) {
    return new ScopusAuthorMapping(
        scopusAuthorId,
        cristinPersonId,
        null,
        null,
        null,
        null,
        false,
        ScopusMappingSource.CURATOR,
        Instant.parse("2026-11-04T12:00:00.000Z"),
        "1234567@20754.0.0.0",
        1,
        false);
  }
}
