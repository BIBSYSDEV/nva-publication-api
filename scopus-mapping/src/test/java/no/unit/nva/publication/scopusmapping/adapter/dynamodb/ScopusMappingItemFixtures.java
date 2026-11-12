package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AUTHOR;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAuthorMapping;
import static org.junit.jupiter.api.Named.named;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** DynamoDB items shared by the tests of the DynamoDB adapter. */
final class ScopusMappingItemFixtures {

  static final String FIXTURES =
      "no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures";
  static final String TYPE = "type";
  static final String AFFILIATION_TYPE = "ScopusAffiliationMapping";
  static final String CRISTIN_PERSON_IDENTIFIER = "cristinPersonIdentifier";
  static final String SOURCE = "source";
  static final String UPDATED_AT = "updatedAt";
  static final String REVISION = "revision";
  private static final String NO_WHOLE_NUMBER_REVISION = "has no whole-number revision";

  private ScopusMappingItemFixtures() {}

  static Map<String, AttributeValue> migratedAuthorItem() {
    return ScopusAuthorMappingDao.current(migratedAuthorMapping(SCOPUS_AUTHOR)).toItem();
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

  static Arguments malformed(
      String description, Map<String, AttributeValue> malformedItem, String expectedReason) {
    return Arguments.of(named(description, malformedItem), expectedReason);
  }
}
