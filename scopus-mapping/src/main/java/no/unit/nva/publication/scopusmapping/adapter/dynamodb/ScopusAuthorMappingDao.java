package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingKeys.CURRENT_SORT_KEY;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingKeys.authorIdentifierOf;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingKeys.authorPartitionKey;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingKeys.historySortKey;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.Map;
import no.unit.nva.publication.scopusmapping.domain.CristinPersonIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusCorrectionContext;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingHistoryEntry;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A stored author mapping item. The PIA migration script writes the same items, so any change here
 * must be made there too (see the golden file described in the module README).
 */
record ScopusAuthorMappingDao(
    @JsonProperty(PRIMARY_KEY_PARTITION_KEY_NAME) String partitionKey,
    @JsonProperty(PRIMARY_KEY_SORT_KEY_NAME) String sortKey,
    String type,
    String cristinPersonIdentifier,
    String orcid,
    @JsonUnwrapped MappingAttributesDao attributes,
    @JsonUnwrapped HistoryAttributesDao history) {

  static final String TYPE = "ScopusAuthorMapping";

  static ScopusAuthorMappingDao current(ScopusAuthorMapping mapping) {
    return of(mapping, CURRENT_SORT_KEY, null);
  }

  /** The immutable copy of a version that a curator's correction wrote. */
  static ScopusAuthorMappingDao history(
      ScopusAuthorMapping mapping, ScopusCorrectionContext correctionContext) {
    return of(
        mapping, historySortKey(mapping.version()), HistoryAttributesDao.of(correctionContext));
  }

  static ScopusAuthorMappingDao fromItem(Map<String, AttributeValue> item) {
    return ScopusMappingItems.fromItem(item, ScopusAuthorMappingDao.class);
  }

  Map<String, AttributeValue> toItem() {
    return ScopusMappingItems.toItem(this);
  }

  /** Builds the mapping, which checks every identifier and invariant of the stored item. */
  ScopusAuthorMapping toMapping() {
    if (!TYPE.equals(type)) {
      throw new IllegalArgumentException(
          "Scopus mapping item %s has type %s, not %s".formatted(partitionKey, type, TYPE));
    }
    return new ScopusAuthorMapping(
        authorIdentifierOf(partitionKey),
        new CristinPersonIdentifier(cristinPersonIdentifier),
        attributes.scopusName(),
        orcid,
        attributes.reviewNeeded(),
        attributes.toPiaMappingCounts(),
        attributes.toVersion());
  }

  ScopusMappingHistoryEntry<ScopusAuthorMapping> toHistoryEntry() {
    return new ScopusMappingHistoryEntry<>(
        toMapping(), history.toCorrectionContext(), history.candidatesOrEmpty());
  }

  private static ScopusAuthorMappingDao of(
      ScopusAuthorMapping mapping, String sortKey, HistoryAttributesDao history) {
    return new ScopusAuthorMappingDao(
        authorPartitionKey(mapping.scopusAuthorIdentifier()),
        sortKey,
        TYPE,
        mapping.cristinPersonIdentifier().value(),
        mapping.orcid(),
        MappingAttributesDao.of(
            mapping.scopusName(),
            mapping.needsReview(),
            mapping.piaMappingCounts(),
            mapping.version()),
        history);
  }
}
