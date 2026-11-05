package no.unit.nva.publication.scopusmapping;

import static java.util.Map.entry;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.FIXTURES;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.MIGRATED_AT;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.MIGRATED_CRISTIN_PERSON_ID;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.MIGRATION_MARKER;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AFFILIATION_ID;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AUTHOR_ID;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.affiliationItem;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.curatorAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.with;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.affiliationCurrentKey;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.authorCurrentKey;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toAffiliationMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toCurrentItem;
import static no.unit.nva.publication.scopusmapping.ScopusMappingSource.PIA_MIGRATION;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromBool;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class ScopusMappingItemsTest {

  private static final String CURRENT = "CURRENT";
  private static final String DELETED = "deleted";
  private static final String UPDATED_AT = "updatedAt";
  private static final Instant CORRECTED_AT = Instant.parse("2026-11-04T12:00:00Z");

  @Test
  void shouldWriteEveryAttributeOfMigratedMappingAtTopLevel() {
    assertThat(toCurrentItem(migratedAuthorMapping(SCOPUS_AUTHOR_ID)))
        .isEqualTo(
            Map.ofEntries(
                entry(PRIMARY_KEY_PARTITION_KEY_NAME, fromS("AUTHOR#SCOPUS#" + SCOPUS_AUTHOR_ID)),
                entry(PRIMARY_KEY_SORT_KEY_NAME, fromS(CURRENT)),
                entry("type", fromS("ScopusAuthorMapping")),
                entry("cristinPersonId", fromS(MIGRATED_CRISTIN_PERSON_ID)),
                entry("scopusName", fromS("Nordmann, Ola")),
                entry("orcid", fromS("0000-0002-1825-0097")),
                entry("mappingCount", fromN("5")),
                entry("totalMappingCount", fromN("7")),
                entry("needsReview", fromBool(true)),
                entry("source", fromS("PIA_MIGRATION")),
                entry(UPDATED_AT, fromS("2026-11-02T08:00:00.000Z")),
                entry("updatedBy", fromS(MIGRATION_MARKER)),
                entry("revision", fromN("1"))));
  }

  @Test
  void shouldLeaveOutAbsentAttributesAndFalseFlags() {
    assertThat(toCurrentItem(curatorMapping(false)))
        .doesNotContainKeys(
            "scopusName", "orcid", "mappingCount", "totalMappingCount", "needsReview", DELETED);
  }

  @Test
  void shouldWriteDeletedOnlyWhenTrue() {
    assertThat(toCurrentItem(curatorMapping(true))).containsEntry(DELETED, fromBool(true));
  }

  @Test
  void shouldWriteUpdatedAtWithMilliseconds() {
    assertThat(toCurrentItem(curatorMapping(false)))
        .containsEntry(UPDATED_AT, fromS("2026-11-04T12:00:00.000Z"));
  }

  @Test
  void shouldKeyCurrentItemsOnTheScopusId() {
    assertThat(authorCurrentKey("1"))
        .isEqualTo(
            Map.of(
                PRIMARY_KEY_PARTITION_KEY_NAME,
                fromS("AUTHOR#SCOPUS#1"),
                PRIMARY_KEY_SORT_KEY_NAME,
                fromS(CURRENT)));
    assertThat(affiliationCurrentKey("2"))
        .isEqualTo(
            Map.of(
                PRIMARY_KEY_PARTITION_KEY_NAME,
                fromS("AFFILIATION#SCOPUS#2"),
                PRIMARY_KEY_SORT_KEY_NAME,
                fromS(CURRENT)));
  }

  @Test
  void shouldReadBackTheAuthorMappingItWrites() {
    var mappings =
        List.of(
            migratedAuthorMapping(SCOPUS_AUTHOR_ID), curatorMapping(false), curatorMapping(true));

    assertThat(mappings)
        .allSatisfy(
            mapping -> assertThat(toAuthorMapping(toCurrentItem(mapping))).isEqualTo(mapping));
  }

  @Test
  void shouldReadAffiliationItem() {
    assertThat(toAffiliationMapping(affiliationItem(SCOPUS_AFFILIATION_ID, "185", "185.15.2.10")))
        .isEqualTo(
            new ScopusAffiliationMapping(
                SCOPUS_AFFILIATION_ID,
                "185",
                "185.15.2.10",
                null,
                null,
                null,
                false,
                PIA_MIGRATION,
                MIGRATED_AT,
                MIGRATION_MARKER,
                1,
                false));
  }

  @Test
  void shouldIgnoreUnknownAttributes() {
    var mapping = migratedAuthorMapping(SCOPUS_AUTHOR_ID);
    var item = with(toCurrentItem(mapping), "addedByTheMigrationScriptLater", fromS("some value"));

    assertThat(toAuthorMapping(item)).isEqualTo(mapping);
  }

  @Test
  void shouldCoerceAttributeOfAnotherTypeLikeTheOtherDaosDo() {
    var mapping = migratedAuthorMapping(SCOPUS_AUTHOR_ID);
    var item = with(toCurrentItem(mapping), "needsReview", fromS("true"));

    assertThat(toAuthorMapping(item)).isEqualTo(mapping);
  }

  @ParameterizedTest
  @MethodSource(FIXTURES + "#malformedAuthorItems")
  void shouldRejectMalformedAuthorItem(
      Map<String, AttributeValue> malformedItem, String expectedReason) {
    assertThatThrownBy(() -> toAuthorMapping(malformedItem)).hasMessageContaining(expectedReason);
  }

  @ParameterizedTest
  @MethodSource(FIXTURES + "#malformedAffiliationItems")
  void shouldRejectMalformedAffiliationItem(
      Map<String, AttributeValue> malformedItem, String expectedReason) {
    assertThatThrownBy(() -> toAffiliationMapping(malformedItem))
        .hasMessageContaining(expectedReason);
  }

  private static ScopusAuthorMapping curatorMapping(boolean deleted) {
    return curatorAuthorMapping(SCOPUS_AUTHOR_ID, "1002", CORRECTED_AT, 2, deleted);
  }
}
