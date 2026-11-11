package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static java.util.Map.entry;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.CURATOR_USERNAME;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.MIGRATED_PERSON;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.MIGRATION_MARKER;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AUTHOR;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.curatorAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedVersion;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.FIXTURES;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.migratedAuthorItem;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.with;
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
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class ScopusAuthorMappingDaoTest {

  private static final String DELETED = "deleted";
  private static final String UPDATED_AT = "updatedAt";
  private static final Instant CORRECTED_AT = Instant.parse("2026-11-04T12:00:00Z");

  @Test
  void shouldStoreEveryValueOfMigratedMappingAsTopLevelAttribute() {
    assertThat(migratedAuthorItem())
        .isEqualTo(
            Map.ofEntries(
                entry(
                    PRIMARY_KEY_PARTITION_KEY_NAME,
                    fromS("AUTHOR#SCOPUS#" + SCOPUS_AUTHOR.value())),
                entry(PRIMARY_KEY_SORT_KEY_NAME, fromS("CURRENT")),
                entry("type", fromS("ScopusAuthorMapping")),
                entry("cristinPersonIdentifier", fromS("1001")),
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
  void shouldLeaveOutAbsentValuesAndFalseFlags() {
    assertThat(currentItem(curatorMapping(false)))
        .containsOnlyKeys(
            PRIMARY_KEY_PARTITION_KEY_NAME,
            PRIMARY_KEY_SORT_KEY_NAME,
            "type",
            "cristinPersonIdentifier",
            "source",
            UPDATED_AT,
            "updatedBy",
            "revision");
  }

  @Test
  void shouldStoreDeletedOnlyWhenTrue() {
    assertThat(currentItem(curatorMapping(true))).containsEntry(DELETED, fromBool(true));
  }

  @Test
  void shouldStoreUpdatedAtWithMilliseconds() {
    assertThat(currentItem(curatorMapping(false)))
        .containsEntry(UPDATED_AT, fromS("2026-11-04T12:00:00.000Z"))
        .containsEntry("updatedBy", fromS(CURATOR_USERNAME));
  }

  @Test
  void shouldReadBackTheMappingItStores() {
    var mappings =
        List.of(migratedAuthorMapping(SCOPUS_AUTHOR), curatorMapping(false), curatorMapping(true));

    assertThat(mappings)
        .allSatisfy(mapping -> assertThat(toMapping(currentItem(mapping))).isEqualTo(mapping));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Ødegård, Åse",
        "Sæther-Bjørnstad, Anne-Marie",
        "Müller, José François",
        "李伟",
        "محمد علي",
        "Ó Briain, Seán \"Jack\"",
        "O'Brien, D'Arcy",
        "Back\\slash, Tab\tand\nnewline",
        "  padded  "
      })
  void shouldKeepTheNameOfTheScopusAuthorExactlyAsItIs(String scopusName) {
    var mapping =
        new ScopusAuthorMapping(
            SCOPUS_AUTHOR, MIGRATED_PERSON, scopusName, null, false, null, migratedVersion());

    assertThat(currentItem(mapping)).containsEntry("scopusName", fromS(scopusName));
    assertThat(toMapping(currentItem(mapping))).isEqualTo(mapping);
  }

  @Test
  void shouldIgnoreUnknownAttributes() {
    var item = with(migratedAuthorItem(), "addedByTheMigrationScriptLater", fromS("some value"));

    assertThat(toMapping(item)).isEqualTo(migratedAuthorMapping(SCOPUS_AUTHOR));
  }

  @Test
  void shouldCoerceAttributeOfAnotherTypeLikeTheOtherDaosDo() {
    var item = with(migratedAuthorItem(), "needsReview", fromS("true"));

    assertThat(toMapping(item)).isEqualTo(migratedAuthorMapping(SCOPUS_AUTHOR));
  }

  @ParameterizedTest
  @MethodSource(FIXTURES + "#malformedAuthorItems")
  void shouldRejectMalformedItem(Map<String, AttributeValue> malformedItem, String expectedReason) {
    assertThatThrownBy(() -> toMapping(malformedItem)).hasMessageContaining(expectedReason);
  }

  private static Map<String, AttributeValue> currentItem(ScopusAuthorMapping mapping) {
    return ScopusAuthorMappingDao.current(mapping).toItem();
  }

  private static ScopusAuthorMapping toMapping(Map<String, AttributeValue> item) {
    return ScopusAuthorMappingDao.fromItem(item).toMapping();
  }

  private static ScopusAuthorMapping curatorMapping(boolean deleted) {
    return curatorAuthorMapping(SCOPUS_AUTHOR, "1002", CORRECTED_AT, 2, deleted);
  }
}
