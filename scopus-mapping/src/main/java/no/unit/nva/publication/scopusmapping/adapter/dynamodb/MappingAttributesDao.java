package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static java.util.Objects.isNull;

import java.time.Instant;
import java.util.Optional;
import no.unit.nva.publication.scopusmapping.domain.PiaMappingCounts;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingSource;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingVersion;

/**
 * The stored attributes that author and affiliation mapping items have in common. Absent values are
 * left out of the item, and the two flags are only stored when true.
 */
record MappingAttributesDao(
    String scopusName,
    Integer mappingCount,
    Integer totalMappingCount,
    Boolean needsReview,
    ScopusMappingSource source,
    String updatedAt,
    String updatedBy,
    Integer revision,
    Boolean deleted) {

  static MappingAttributesDao of(
      String scopusName,
      boolean needsReview,
      PiaMappingCounts piaMappingCounts,
      ScopusMappingVersion version) {
    var counts = Optional.ofNullable(piaMappingCounts);
    return new MappingAttributesDao(
        scopusName,
        counts.map(PiaMappingCounts::mappingCount).orElse(null),
        counts.map(PiaMappingCounts::totalMappingCount).orElse(null),
        onlyWhenTrue(needsReview),
        version.source(),
        ScopusMappingKeys.timestamp(version.updatedAt()),
        version.updatedBy(),
        version.revision(),
        onlyWhenTrue(version.deleted()));
  }

  boolean reviewNeeded() {
    return Boolean.TRUE.equals(needsReview);
  }

  PiaMappingCounts toPiaMappingCounts() {
    return isNull(mappingCount) && isNull(totalMappingCount)
        ? null
        : new PiaMappingCounts(mappingCount, totalMappingCount);
  }

  ScopusMappingVersion toVersion() {
    return new ScopusMappingVersion(
        source, Instant.parse(updatedAt), updatedBy, revision, Boolean.TRUE.equals(deleted));
  }

  private static Boolean onlyWhenTrue(boolean flag) {
    return flag ? Boolean.TRUE : null;
  }
}
