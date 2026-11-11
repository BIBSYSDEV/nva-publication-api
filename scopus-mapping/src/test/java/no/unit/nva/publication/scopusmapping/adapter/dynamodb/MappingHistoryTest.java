package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static java.util.Collections.emptyList;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.MIGRATED_AT;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AFFILIATION;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AUTHOR;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.curatorAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAffiliationMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAuthorMapping;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.CRISTIN_PERSON_IDENTIFIER;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.REVISION;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.migratedAffiliationItem;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.with;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.without;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromBool;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromL;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromM;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusCorrectionContext;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingHistoryEntry;
import nva.commons.logutils.LogRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class MappingHistoryTest extends ScopusMappingLocalTest {

  private static final String CANDIDATES = "candidates";
  private static final String MIGRATED_HISTORY_SORT_KEY = "HISTORY#2026-11-02T08:00:00.000Z#1";
  private static final ScopusCorrectionContext CORRECTION_CONTEXT =
      new ScopusCorrectionContext("2-s2.0-85200000010", 1);

  @Test
  void shouldKeepEachVersionUnderItsTimestampAndRevisionWithWhereItWasCorrected() {
    var correction =
        curatorAuthorMapping(SCOPUS_AUTHOR, "1002", MIGRATED_AT.plusSeconds(60), 2, false);

    assertThat(historyItem(correction))
        .containsEntry(PRIMARY_KEY_SORT_KEY_NAME, fromS("HISTORY#2026-11-02T08:01:00.000Z#2"))
        .containsEntry("scopusPublicationIdentifier", fromS("2-s2.0-85200000010"))
        .containsEntry("sequenceNumber", fromN("1"))
        .doesNotContainKey(CANDIDATES);
  }

  @Test
  void shouldReturnVersionsByRevisionEvenWhenTimestampsAreOutOfOrder() {
    var firstVersion = migratedAuthorMapping(SCOPUS_AUTHOR);
    var secondVersion =
        curatorAuthorMapping(SCOPUS_AUTHOR, "1002", MIGRATED_AT.minusSeconds(60), 2, false);
    putItem(historyItem(secondVersion));
    putItem(historyItem(firstVersion));

    assertThat(service.findAuthorMappingHistory(SCOPUS_AUTHOR))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(firstVersion, CORRECTION_CONTEXT, emptyList()),
            new ScopusMappingHistoryEntry<>(secondVersion, CORRECTION_CONTEXT, emptyList()));
  }

  @Test
  void shouldReadCandidateValuesAsText() {
    var candidate =
        fromM(
            Map.of(
                CRISTIN_PERSON_IDENTIFIER,
                fromS("1001"),
                "publicationCount",
                fromN("4"),
                "stillActive",
                fromBool(true)));
    putItem(
        with(
            historyItem(migratedAuthorMapping(SCOPUS_AUTHOR)),
            CANDIDATES,
            fromL(List.of(candidate))));

    assertThat(service.findAuthorMappingHistory(SCOPUS_AUTHOR))
        .singleElement()
        .extracting(ScopusMappingHistoryEntry::candidates)
        .isEqualTo(
            List.of(
                Map.of(
                    CRISTIN_PERSON_IDENTIFIER,
                    "1001",
                    "publicationCount",
                    "4",
                    "stillActive",
                    "true")));
  }

  @Test
  void shouldReadMigratedAffiliationVersionWithoutCorrectionContext() {
    var institution = "194.0.0.0";
    putItem(
        with(
            migratedAffiliationItem(SCOPUS_AFFILIATION, institution, institution),
            PRIMARY_KEY_SORT_KEY_NAME,
            fromS(MIGRATED_HISTORY_SORT_KEY)));

    assertThat(service.findAffiliationMappingHistory(SCOPUS_AFFILIATION))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(
                migratedAffiliationMapping(institution, institution), null, emptyList()));
  }

  @Test
  void shouldReturnEmptyHistoryForIdentifierWithoutMapping() {
    assertThat(service.findAuthorMappingHistory(SCOPUS_AUTHOR)).isEmpty();
    assertThat(service.findAffiliationMappingHistory(new ScopusAffiliationIdentifier("99990301")))
        .isEmpty();
  }

  @ParameterizedTest
  @MethodSource("malformedHistoryItems")
  void shouldLeaveOutAndLogMalformedVersionButReadTheOtherVersions(
      Map<String, AttributeValue> malformedSecondVersion, String expectedReason) {
    var logRecorder = LogRecorder.forClass(ScopusMappingItems.class);
    var firstVersion = migratedAuthorMapping(SCOPUS_AUTHOR);
    putItem(historyItem(firstVersion));
    putItem(malformedSecondVersion);

    assertThat(service.findAuthorMappingHistory(SCOPUS_AUTHOR))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(firstVersion, CORRECTION_CONTEXT, emptyList()));
    assertThat(logRecorder.messages())
        .singleElement()
        .asString()
        .contains("HISTORY#2026-11-02T08:01:00.000Z#2")
        .contains(expectedReason);
  }

  private static Stream<Arguments> malformedHistoryItems() {
    var secondVersion =
        historyItem(
            curatorAuthorMapping(SCOPUS_AUTHOR, "1002", MIGRATED_AT.plusSeconds(60), 2, false));
    var candidateWithNestedList = fromM(Map.of("nested", fromL(List.of(fromS("value")))));
    return Stream.of(
        Arguments.of(
            named(
                "missing cristinPersonIdentifier",
                without(secondVersion, CRISTIN_PERSON_IDENTIFIER)),
            "Cristin person identifier"),
        Arguments.of(
            named("revision stored as a string", with(secondVersion, REVISION, fromS("2"))),
            "revision"),
        Arguments.of(
            named(
                "candidates that are not a list",
                with(secondVersion, CANDIDATES, fromS("not a list"))),
            "Cannot deserialize"),
        Arguments.of(
            named(
                "candidate with a nested list",
                with(secondVersion, CANDIDATES, fromL(List.of(candidateWithNestedList)))),
            "Cannot deserialize"));
  }

  private static Map<String, AttributeValue> historyItem(ScopusAuthorMapping mapping) {
    return ScopusAuthorMappingDao.history(mapping, CORRECTION_CONTEXT).toItem();
  }
}
