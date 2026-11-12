package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingKeys.affiliationIdentifierOf;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.Map;
import no.unit.nva.publication.scopusmapping.domain.CristinOrganizationIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationMapping;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A stored affiliation mapping item. Only the PIA migration script writes these, so this DAO only
 * reads them. Any change here must be made in the script too (see the golden file described in the
 * module README).
 */
record ScopusAffiliationMappingDao(
    @JsonProperty(PRIMARY_KEY_PARTITION_KEY_NAME) String partitionKey,
    @JsonProperty(PRIMARY_KEY_SORT_KEY_NAME) String sortKey,
    String type,
    String cristinInstitutionIdentifier,
    String cristinUnitIdentifier,
    @JsonUnwrapped MappingAttributesDao attributes) {

  static final String TYPE = "ScopusAffiliationMapping";

  static ScopusAffiliationMappingDao fromItem(Map<String, AttributeValue> item) {
    return ScopusMappingItems.fromItem(item, ScopusAffiliationMappingDao.class);
  }

  /** Builds the mapping, which checks every identifier and invariant of the stored item. */
  ScopusAffiliationMapping toMapping() {
    if (!TYPE.equals(type)) {
      throw new IllegalArgumentException(
          "Scopus mapping item %s has type %s, not %s".formatted(partitionKey, type, TYPE));
    }
    return new ScopusAffiliationMapping(
        affiliationIdentifierOf(partitionKey),
        new CristinOrganizationIdentifier(cristinInstitutionIdentifier),
        new CristinOrganizationIdentifier(cristinUnitIdentifier),
        attributes.scopusName(),
        attributes.reviewNeeded(),
        attributes.toPiaMappingCounts(),
        attributes.toVersion());
  }
}
