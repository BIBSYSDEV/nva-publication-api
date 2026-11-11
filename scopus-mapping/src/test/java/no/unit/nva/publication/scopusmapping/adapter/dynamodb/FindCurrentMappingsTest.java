package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static java.util.Collections.emptyList;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.MIGRATED_AT;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AFFILIATION;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AUTHOR;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.curatorAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAffiliationMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAuthorMapping;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.FIXTURES;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.migratedAffiliationItem;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItems.partitionKeyOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorMapping;
import nva.commons.logutils.LogRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class FindCurrentMappingsTest extends ScopusMappingLocalTest {

  private static final ScopusAuthorIdentifier OTHER_AUTHOR =
      new ScopusAuthorIdentifier("99999990202");
  private static final ScopusAffiliationIdentifier OTHER_AFFILIATION =
      new ScopusAffiliationIdentifier("99990202");
  private static final String INSTITUTION = "194.0.0.0";

  @Test
  void shouldFindAuthorAndAffiliationMappings() {
    var authorMapping = migratedAuthorMapping(SCOPUS_AUTHOR);
    putCurrent(authorMapping);
    putItem(migratedAffiliationItem(SCOPUS_AFFILIATION, INSTITUTION, "194.63.10.0"));

    var mappings =
        service.findCurrentMappings(
            List.of(SCOPUS_AUTHOR, OTHER_AUTHOR), List.of(SCOPUS_AFFILIATION, OTHER_AFFILIATION));

    assertThat(mappings.authorMappings()).isEqualTo(Map.of(SCOPUS_AUTHOR, authorMapping));
    assertThat(mappings.authorMapping(OTHER_AUTHOR)).isEmpty();
    assertThat(mappings.affiliationMapping(SCOPUS_AFFILIATION))
        .contains(migratedAffiliationMapping(INSTITUTION, "194.63.10.0"));
    assertThat(mappings.affiliationMapping(OTHER_AFFILIATION)).isEmpty();
  }

  @Test
  void shouldTreatDeletedMappingAsNoMapping() {
    putCurrent(curatorAuthorMapping(SCOPUS_AUTHOR, "1002", MIGRATED_AT, 2, true));

    var mappings = service.findCurrentMappings(List.of(SCOPUS_AUTHOR), emptyList());

    assertThat(mappings.authorMapping(SCOPUS_AUTHOR)).isEmpty();
  }

  @Test
  void shouldFindMoreMappingsThanFitInOneBatchGetItem() {
    var scopusAuthors =
        IntStream.range(0, 150)
            .mapToObj(index -> new ScopusAuthorIdentifier("999999" + (10_000 + index)))
            .toList();
    scopusAuthors.forEach(scopusAuthor -> putCurrent(migratedAuthorMapping(scopusAuthor)));

    var mappings = service.findCurrentMappings(scopusAuthors, List.of(OTHER_AFFILIATION));

    assertThat(mappings.authorMappings()).containsOnlyKeys(scopusAuthors);
    assertThat(mappings.affiliationMappings()).isEmpty();
  }

  @Test
  void shouldAcceptIdentifierGivenMoreThanOnce() {
    var mapping = migratedAuthorMapping(SCOPUS_AUTHOR);
    putCurrent(mapping);

    var mappings =
        service.findCurrentMappings(
            List.of(SCOPUS_AUTHOR, SCOPUS_AUTHOR), List.of(OTHER_AFFILIATION, OTHER_AFFILIATION));

    assertThat(mappings.authorMappings()).isEqualTo(Map.of(SCOPUS_AUTHOR, mapping));
  }

  @Test
  void shouldFindNoMappingsWhenAskedForNoIdentifiers() {
    var mappings = service.findCurrentMappings(emptyList(), emptyList());

    assertThat(mappings.authorMappings()).isEmpty();
    assertThat(mappings.affiliationMappings()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource(FIXTURES + "#malformedCurrentItems")
  void shouldLeaveOutAndLogMalformedItemButFindTheOtherMappings(
      Map<String, AttributeValue> malformedItem, String expectedReason) {
    var logRecorder = LogRecorder.forClass(ScopusMappingItems.class);
    var otherAuthorMapping = migratedAuthorMapping(OTHER_AUTHOR);
    putItem(malformedItem);
    putCurrent(otherAuthorMapping);
    putItem(migratedAffiliationItem(OTHER_AFFILIATION, INSTITUTION, INSTITUTION));

    var mappings =
        service.findCurrentMappings(
            List.of(SCOPUS_AUTHOR, OTHER_AUTHOR), List.of(SCOPUS_AFFILIATION, OTHER_AFFILIATION));

    assertThat(mappings.authorMappings()).isEqualTo(Map.of(OTHER_AUTHOR, otherAuthorMapping));
    assertThat(mappings.affiliationMappings()).containsOnlyKeys(OTHER_AFFILIATION);
    assertThat(logRecorder.messages())
        .singleElement()
        .asString()
        .contains(partitionKeyOf(malformedItem) + " CURRENT")
        .contains(expectedReason);
  }

  private void putCurrent(ScopusAuthorMapping mapping) {
    putItem(ScopusAuthorMappingDao.current(mapping).toItem());
  }
}
