package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AFFILIATION;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAffiliationMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedVersion;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.FIXTURES;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.migratedAffiliationItem;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.with;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromBool;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.util.Map;
import no.unit.nva.publication.scopusmapping.domain.CristinOrganizationIdentifier;
import no.unit.nva.publication.scopusmapping.domain.PiaMappingCounts;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class ScopusAffiliationMappingDaoTest {

  private static final String INSTITUTION = "185.0.0.0";
  private static final String SUB_UNIT = "185.15.2.10";

  @Test
  void shouldReadItemWithOnlyTheRequiredValues() {
    var item = migratedAffiliationItem(SCOPUS_AFFILIATION, INSTITUTION, SUB_UNIT);

    assertThat(toMapping(item)).isEqualTo(migratedAffiliationMapping(INSTITUTION, SUB_UNIT));
  }

  @Test
  void shouldReadMappingAtInstitutionLevelWithTheInstitutionAsItsUnit() {
    var item = migratedAffiliationItem(SCOPUS_AFFILIATION, INSTITUTION, INSTITUTION);

    assertThat(toMapping(item).cristinUnitIdentifier())
        .isEqualTo(new CristinOrganizationIdentifier(INSTITUTION));
  }

  @Test
  void shouldReadEveryOptionalValue() {
    var item = migratedAffiliationItem(SCOPUS_AFFILIATION, INSTITUTION, SUB_UNIT);
    var itemWithScopusName = with(item, "scopusName", fromS("University of Oslo"));
    var itemWithCounts =
        with(with(itemWithScopusName, "mappingCount", fromN("3")), "totalMappingCount", fromN("5"));
    var itemWithEveryValue = with(itemWithCounts, "needsReview", fromBool(true));

    assertThat(toMapping(itemWithEveryValue))
        .isEqualTo(
            new ScopusAffiliationMapping(
                SCOPUS_AFFILIATION,
                new CristinOrganizationIdentifier(INSTITUTION),
                new CristinOrganizationIdentifier(SUB_UNIT),
                "University of Oslo",
                true,
                new PiaMappingCounts(3, 5),
                migratedVersion()));
  }

  @ParameterizedTest
  @MethodSource(FIXTURES + "#malformedAffiliationItems")
  void shouldRejectMalformedItem(Map<String, AttributeValue> malformedItem, String expectedReason) {
    assertThatThrownBy(() -> toMapping(malformedItem)).hasMessageContaining(expectedReason);
  }

  private static ScopusAffiliationMapping toMapping(Map<String, AttributeValue> item) {
    return ScopusAffiliationMappingDao.fromItem(item).toMapping();
  }
}
