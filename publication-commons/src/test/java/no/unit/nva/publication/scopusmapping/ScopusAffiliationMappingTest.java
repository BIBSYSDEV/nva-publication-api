package no.unit.nva.publication.scopusmapping;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ScopusAffiliationMappingTest {

  private static final String SCOPUS_AFFILIATION_ID = "99990401";
  private static final String CRISTIN_INSTITUTION_ID = "194";
  private static final String TOP_LEVEL_UNIT_ID = "194.0.0.0";

  @ParameterizedTest
  @ValueSource(strings = {TOP_LEVEL_UNIT_ID, "194.12.3.40"})
  void shouldAcceptUnitOfTheInstitution(String cristinUnitId) {
    assertThatCode(
            () -> affiliationMapping(SCOPUS_AFFILIATION_ID, CRISTIN_INSTITUTION_ID, cristinUnitId))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(strings = {"185.0.0.0", "1940.0.0.0", "19.0.0.0", "194", "194.0.0", "194.0.0.0.0"})
  void shouldRejectUnitOutsideTheInstitutionOrWithoutFourParts(String cristinUnitId) {
    assertThatThrownBy(
            () -> affiliationMapping(SCOPUS_AFFILIATION_ID, CRISTIN_INSTITUTION_ID, cristinUnitId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(cristinUnitId);
  }

  @ParameterizedTest
  @ValueSource(strings = {"SCOPUS:99990401", "9999O401", ""})
  void shouldRejectScopusAffiliationIdThatIsNotNumeric(String scopusAffiliationId) {
    assertThatThrownBy(
            () ->
                affiliationMapping(scopusAffiliationId, CRISTIN_INSTITUTION_ID, TOP_LEVEL_UNIT_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("scopusAffiliationId");
  }

  @ParameterizedTest
  @ValueSource(strings = {"https://api.nva.unit.no/cristin/organization/194", "NTNU", ""})
  void shouldRejectCristinInstitutionIdThatIsNotNumeric(String cristinInstitutionId) {
    assertThatThrownBy(
            () ->
                affiliationMapping(SCOPUS_AFFILIATION_ID, cristinInstitutionId, TOP_LEVEL_UNIT_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cristinInstitutionId");
  }

  private static ScopusAffiliationMapping affiliationMapping(
      String scopusAffiliationId, String cristinInstitutionId, String cristinUnitId) {
    return new ScopusAffiliationMapping(
        scopusAffiliationId,
        cristinInstitutionId,
        cristinUnitId,
        null,
        null,
        null,
        false,
        ScopusMappingSource.PIA_MIGRATION,
        Instant.parse("2026-11-02T08:00:00.000Z"),
        "pia-migration",
        1,
        false);
  }
}
