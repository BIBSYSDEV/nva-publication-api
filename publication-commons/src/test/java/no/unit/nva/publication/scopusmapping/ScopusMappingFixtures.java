package no.unit.nva.publication.scopusmapping;

import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.affiliationPartitionKey;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toCurrentItem;
import static no.unit.nva.publication.scopusmapping.ScopusMappingSource.CURATOR;
import static no.unit.nva.publication.scopusmapping.ScopusMappingSource.PIA_MIGRATION;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static org.junit.jupiter.api.Named.named;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Mappings and DynamoDB items shared by the Scopus mapping tests. */
final class ScopusMappingFixtures {

  static final String FIXTURES = "no.unit.nva.publication.scopusmapping.ScopusMappingFixtures";
  static final String TABLE_NAME = "import-candidates";
  static final String SCOPUS_AUTHOR_ID = "99999990201";
  static final String SCOPUS_AFFILIATION_ID = "99990201";
  static final String MIGRATED_CRISTIN_PERSON_ID = "1001";
  static final Instant MIGRATED_AT = Instant.parse("2026-11-02T08:00:00.000Z");
  static final String MIGRATION_MARKER = "pia-migration";
  static final String CURATOR_USERNAME = "1234567@20754.0.0.0";
  private static final String TYPE = "type";
  private static final String AUTHOR = "ScopusAuthorMapping";
  private static final String AFFILIATION = "ScopusAffiliationMapping";
  private static final String CRISTIN_PERSON_ID = "cristinPersonId";
  private static final String SOURCE = "source";
  private static final String UPDATED_AT = "updatedAt";
  private static final String REVISION = "revision";
  private static final String NO_WHOLE_NUMBER_REVISION = "has no whole-number revision";

  private ScopusMappingFixtures() {}

  static ScopusAuthorMapping migratedAuthorMapping(String scopusAuthorId) {
    return new ScopusAuthorMapping(
        scopusAuthorId,
        MIGRATED_CRISTIN_PERSON_ID,
        "Nordmann, Ola",
        "0000-0002-1825-0097",
        5,
        7,
        true,
        PIA_MIGRATION,
        MIGRATED_AT,
        MIGRATION_MARKER,
        1,
        false);
  }

  /** A curator's version of a mapping, with none of the fields that only the migration sets. */
  static ScopusAuthorMapping curatorAuthorMapping(
      String scopusAuthorId,
      String cristinPersonId,
      Instant updatedAt,
      int revision,
      boolean deleted) {
    return new ScopusAuthorMapping(
        scopusAuthorId,
        cristinPersonId,
        null,
        null,
        null,
        null,
        false,
        CURATOR,
        updatedAt,
        CURATOR_USERNAME,
        revision,
        deleted);
  }

  static Map<String, AttributeValue> affiliationItem(
      String scopusAffiliationId, String cristinInstitutionId, String cristinUnitId) {
    return Map.of(
        PRIMARY_KEY_PARTITION_KEY_NAME,
        fromS(affiliationPartitionKey(scopusAffiliationId)),
        PRIMARY_KEY_SORT_KEY_NAME,
        fromS("CURRENT"),
        TYPE,
        fromS(AFFILIATION),
        "cristinInstitutionId",
        fromS(cristinInstitutionId),
        "cristinUnitId",
        fromS(cristinUnitId),
        SOURCE,
        fromS(PIA_MIGRATION.name()),
        UPDATED_AT,
        fromS("2026-11-02T08:00:00.000Z"),
        "updatedBy",
        fromS(MIGRATION_MARKER),
        REVISION,
        fromN("1"));
  }

  static Map<String, AttributeValue> with(
      Map<String, AttributeValue> item, String attributeName, AttributeValue value) {
    var changedItem = new HashMap<>(item);
    changedItem.put(attributeName, value);
    return changedItem;
  }

  static Map<String, AttributeValue> without(
      Map<String, AttributeValue> item, String attributeName) {
    var changedItem = new HashMap<>(item);
    changedItem.remove(attributeName);
    return changedItem;
  }

  static Stream<Arguments> malformedAuthorItems() {
    var authorItem = toCurrentItem(migratedAuthorMapping(SCOPUS_AUTHOR_ID));
    return Stream.concat(
        malformedAuthorItemsWithReadableRevision(),
        Stream.of(
            malformed(
                "revision stored as a string",
                with(authorItem, REVISION, fromS("1")),
                NO_WHOLE_NUMBER_REVISION),
            malformed(
                "revision that is not a whole number",
                with(authorItem, REVISION, fromN("1.5")),
                NO_WHOLE_NUMBER_REVISION),
            malformed(
                "missing revision", without(authorItem, REVISION), NO_WHOLE_NUMBER_REVISION)));
  }

  static Stream<Arguments> malformedAuthorItemsWithReadableRevision() {
    var authorItem = toCurrentItem(migratedAuthorMapping(SCOPUS_AUTHOR_ID));
    return Stream.of(
        malformed(
            "unknown source", with(authorItem, SOURCE, fromS("SOMETHING_NEW")), "SOMETHING_NEW"),
        malformed(
            "missing cristinPersonId", without(authorItem, CRISTIN_PERSON_ID), CRISTIN_PERSON_ID),
        malformed(
            "cristinPersonId that is a URI",
            with(authorItem, CRISTIN_PERSON_ID, fromS("https://api.nva.unit.no/cristin/person/1")),
            "cristinPersonId must be a plain numeric id"),
        malformed(
            "updatedAt that is not an instant",
            with(authorItem, UPDATED_AT, fromS("today")),
            "today"),
        malformed("missing type", without(authorItem, TYPE), "not ScopusAuthorMapping"),
        malformed(
            "affiliation type on an author key",
            with(authorItem, TYPE, fromS(AFFILIATION)),
            "has type ScopusAffiliationMapping"));
  }

  static Stream<Arguments> malformedAffiliationItems() {
    return Stream.of(
        malformed(
            "unit outside the institution",
            affiliationItem(SCOPUS_AFFILIATION_ID, "194", "185.0.0.0"),
            "185.0.0.0"),
        malformed(
            "author type on an affiliation key",
            with(affiliationItem(SCOPUS_AFFILIATION_ID, "194", "194.0.0.0"), TYPE, fromS(AUTHOR)),
            "has type ScopusAuthorMapping"));
  }

  private static Arguments malformed(
      String description, Map<String, AttributeValue> malformedItem, String expectedReason) {
    return Arguments.of(named(description, malformedItem), expectedReason);
  }
}
