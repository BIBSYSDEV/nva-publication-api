package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.MIGRATION_MARKER;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AFFILIATION;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AUTHOR;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAuthorMapping;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingKeys.affiliationPartitionKey;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static org.junit.jupiter.api.Named.named;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationIdentifier;
import org.junit.jupiter.params.provider.Arguments;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** DynamoDB items shared by the tests of the DynamoDB adapter. */
final class ScopusMappingItemFixtures {

  static final String FIXTURES =
      "no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures";
  static final String TYPE = "type";
  static final String AUTHOR_TYPE = "ScopusAuthorMapping";
  static final String AFFILIATION_TYPE = "ScopusAffiliationMapping";
  static final String CRISTIN_PERSON_IDENTIFIER = "cristinPersonIdentifier";
  static final String CRISTIN_INSTITUTION_IDENTIFIER = "cristinInstitutionIdentifier";
  static final String CRISTIN_UNIT_IDENTIFIER = "cristinUnitIdentifier";
  static final String SOURCE = "source";
  static final String UPDATED_AT = "updatedAt";
  static final String REVISION = "revision";
  private static final String NO_WHOLE_NUMBER_REVISION = "has no whole-number revision";

  private ScopusMappingItemFixtures() {}

  static Map<String, AttributeValue> migratedAuthorItem() {
    return ScopusAuthorMappingDao.current(migratedAuthorMapping(SCOPUS_AUTHOR)).toItem();
  }

  /** An affiliation item as the PIA migration script writes it, with only the required values. */
  static Map<String, AttributeValue> migratedAffiliationItem(
      ScopusAffiliationIdentifier scopusAffiliationIdentifier,
      String cristinInstitutionIdentifier,
      String cristinUnitIdentifier) {
    return Map.of(
        PRIMARY_KEY_PARTITION_KEY_NAME,
        fromS(affiliationPartitionKey(scopusAffiliationIdentifier)),
        PRIMARY_KEY_SORT_KEY_NAME,
        fromS("CURRENT"),
        TYPE,
        fromS(AFFILIATION_TYPE),
        CRISTIN_INSTITUTION_IDENTIFIER,
        fromS(cristinInstitutionIdentifier),
        CRISTIN_UNIT_IDENTIFIER,
        fromS(cristinUnitIdentifier),
        SOURCE,
        fromS("PIA_MIGRATION"),
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

  static Stream<Arguments> malformedCurrentItems() {
    return Stream.concat(malformedAuthorItems(), malformedAffiliationItems());
  }

  static Stream<Arguments> malformedAuthorItems() {
    var authorItem = migratedAuthorItem();
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
    var authorItem = migratedAuthorItem();
    return Stream.of(
        malformed(
            "unknown source", with(authorItem, SOURCE, fromS("SOMETHING_NEW")), "SOMETHING_NEW"),
        malformed(
            "missing cristinPersonIdentifier",
            without(authorItem, CRISTIN_PERSON_IDENTIFIER),
            "Cristin person identifier"),
        malformed(
            "cristinPersonIdentifier that is a URI",
            with(
                authorItem,
                CRISTIN_PERSON_IDENTIFIER,
                fromS("https://api.nva.unit.no/cristin/person/1")),
            "Cristin person identifier must be a plain number"),
        malformed(
            "updatedAt that is not an instant",
            with(authorItem, UPDATED_AT, fromS("today")),
            "today"),
        malformed("missing type", without(authorItem, TYPE), "has type null"),
        malformed(
            "affiliation type on an author key",
            with(authorItem, TYPE, fromS(AFFILIATION_TYPE)),
            "has type ScopusAffiliationMapping"));
  }

  static Stream<Arguments> malformedAffiliationItems() {
    var affiliationItem = migratedAffiliationItem(SCOPUS_AFFILIATION, "194.0.0.0", "194.63.10.0");
    return Stream.of(
        malformed(
            "unit of another institution",
            with(affiliationItem, CRISTIN_UNIT_IDENTIFIER, fromS("185.15.2.10")),
            "185.15.2.10"),
        malformed(
            "institution as the bare number PIA uses",
            with(affiliationItem, CRISTIN_INSTITUTION_IDENTIFIER, fromS("194")),
            "must be four numbers separated by dots: 194"),
        malformed(
            "institution that is not top-level",
            with(affiliationItem, CRISTIN_INSTITUTION_IDENTIFIER, fromS("194.63.0.0")),
            "not a top-level organization"),
        malformed(
            "missing unit",
            without(affiliationItem, CRISTIN_UNIT_IDENTIFIER),
            "Cristin organization identifier"),
        malformed(
            "author type on an affiliation key",
            with(affiliationItem, TYPE, fromS(AUTHOR_TYPE)),
            "has type ScopusAuthorMapping"));
  }

  static Arguments malformed(
      String description, Map<String, AttributeValue> malformedItem, String expectedReason) {
    return Arguments.of(named(description, malformedItem), expectedReason);
  }
}
