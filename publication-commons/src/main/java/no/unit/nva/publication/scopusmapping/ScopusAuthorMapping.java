package no.unit.nva.publication.scopusmapping;

import static java.util.Objects.requireNonNull;
import static no.unit.nva.publication.scopusmapping.ScopusMappingIds.requireNumericId;

import java.time.Instant;

/**
 * The mapping of a Scopus author id (AUID) to a Cristin person. Cristin ids and ORCIDs are plain
 * ids, never URIs. {@code scopusName}, {@code orcid}, {@code mappingCount} and {@code
 * totalMappingCount} are null when the item does not have them; only the migrated version has the
 * counts.
 */
public record ScopusAuthorMapping(
    String scopusAuthorId,
    String cristinPersonId,
    String scopusName,
    String orcid,
    Integer mappingCount,
    Integer totalMappingCount,
    boolean needsReview,
    ScopusMappingSource source,
    Instant updatedAt,
    String updatedBy,
    int revision,
    boolean deleted) {

  public ScopusAuthorMapping {
    requireNumericId(scopusAuthorId, "scopusAuthorId");
    requireNumericId(cristinPersonId, "cristinPersonId");
    requireNonNull(source, "source");
    requireNonNull(updatedAt, "updatedAt");
    requireNonNull(updatedBy, "updatedBy");
  }
}
