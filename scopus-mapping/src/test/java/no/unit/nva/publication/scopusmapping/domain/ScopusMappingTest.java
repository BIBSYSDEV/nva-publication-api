package no.unit.nva.publication.scopusmapping.domain;

import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.MIGRATED_AT;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.MIGRATED_PERSON;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.MIGRATION_MARKER;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AUTHOR;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAffiliationMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedVersion;
import static no.unit.nva.publication.scopusmapping.domain.ScopusMappingSource.CURATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ScopusMappingTest {

  private static final String INSTITUTION = "194.0.0.0";

  @Test
  void shouldGiveAuthorAndAffiliationMappingsTheirVersionInCommon() {
    ScopusMapping authorMapping = migratedAuthorMapping(SCOPUS_AUTHOR);
    ScopusMapping affiliationMapping = migratedAffiliationMapping(INSTITUTION, INSTITUTION);

    assertThat(authorMapping.version()).isEqualTo(migratedVersion());
    assertThat(affiliationMapping.version()).isEqualTo(migratedVersion());
  }

  @Test
  void shouldRequireAuthorMappingToHaveIdentifiersAndVersion() {
    assertThatNullPointerException()
        .isThrownBy(() -> authorMapping(null, MIGRATED_PERSON, migratedVersion()))
        .withMessage("scopusAuthorIdentifier");
    assertThatNullPointerException()
        .isThrownBy(() -> authorMapping(SCOPUS_AUTHOR, null, migratedVersion()))
        .withMessage("cristinPersonIdentifier");
    assertThatNullPointerException()
        .isThrownBy(() -> authorMapping(SCOPUS_AUTHOR, MIGRATED_PERSON, null))
        .withMessage("version");
  }

  @ParameterizedTest
  @ValueSource(strings = {INSTITUTION, "194.63.10.0"})
  void shouldAcceptUnitOfTheInstitution(String cristinUnitIdentifier) {
    var mapping = migratedAffiliationMapping(INSTITUTION, cristinUnitIdentifier);

    assertThat(mapping.cristinUnitIdentifier().value()).isEqualTo(cristinUnitIdentifier);
  }

  @ParameterizedTest
  @ValueSource(strings = {"185.0.0.0", "1940.0.0.0", "19.0.0.0", "185.15.2.10"})
  void shouldRejectUnitOfAnotherInstitution(String cristinUnitIdentifier) {
    assertThatThrownBy(() -> migratedAffiliationMapping(INSTITUTION, cristinUnitIdentifier))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(cristinUnitIdentifier);
  }

  @Test
  void shouldRejectInstitutionThatIsNotTopLevel() {
    assertThatThrownBy(() -> migratedAffiliationMapping("194.63.0.0", "194.63.10.0"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not a top-level organization");
  }

  @ParameterizedTest
  @CsvSource({"0", "-1"})
  void shouldRejectRevisionBelowTheFirst(int revision) {
    assertThatThrownBy(
            () -> new ScopusMappingVersion(CURATOR, MIGRATED_AT, MIGRATION_MARKER, revision, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("revision");
  }

  @Test
  void shouldRequireVersionToSayWhoWroteItAndWhen() {
    assertThatNullPointerException()
        .isThrownBy(() -> new ScopusMappingVersion(null, MIGRATED_AT, MIGRATION_MARKER, 1, false))
        .withMessage("source");
    assertThatNullPointerException()
        .isThrownBy(() -> new ScopusMappingVersion(CURATOR, null, MIGRATION_MARKER, 1, false))
        .withMessage("updatedAt");
    assertThatNullPointerException()
        .isThrownBy(() -> new ScopusMappingVersion(CURATOR, MIGRATED_AT, null, 1, false))
        .withMessage("updatedBy");
  }

  private static ScopusAuthorMapping authorMapping(
      ScopusAuthorIdentifier scopusAuthorIdentifier,
      CristinPersonIdentifier cristinPersonIdentifier,
      ScopusMappingVersion version) {
    return new ScopusAuthorMapping(
        scopusAuthorIdentifier, cristinPersonIdentifier, null, null, false, null, version);
  }
}
