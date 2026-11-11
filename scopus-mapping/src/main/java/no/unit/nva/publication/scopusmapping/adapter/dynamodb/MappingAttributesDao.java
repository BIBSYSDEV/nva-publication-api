package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Optional;
import no.unit.nva.publication.scopusmapping.domain.PiaMappingCounts;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingSource;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingVersion;

/**
 * The stored attributes that author and affiliation mapping items have in common. Absent values are
 * left out of the item. The two flags are only stored when true, and are false when absent.
 */
record MappingAttributesDao(
    String scopusName,
    Integer mappingCount,
    Integer totalMappingCount,
    @JsonInclude(NON_DEFAULT) boolean needsReview,
    ScopusMappingSource source,
    String updatedAt,
    String updatedBy,
    Integer revision,
    @JsonInclude(NON_DEFAULT) boolean deleted) {

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
        needsReview,
        version.source(),
        ScopusMappingKeys.timestamp(version.updatedAt()),
        version.updatedBy(),
        version.revision(),
        version.deleted());
  }

  PiaMappingCounts toPiaMappingCounts() {
    return isNull(mappingCount) && isNull(totalMappingCount)
        ? null
        : new PiaMappingCounts(mappingCount, totalMappingCount);
  }

  ScopusMappingVersion toVersion() {
    return new ScopusMappingVersion(source, Instant.parse(updatedAt), updatedBy, revision, deleted);
  }
}
