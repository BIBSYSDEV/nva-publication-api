package no.unit.nva.publication.scopusmapping;

import static java.util.Objects.requireNonNull;
import static no.unit.nva.publication.scopusmapping.ScopusMappingIds.requireNumericId;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * The mapping of a Scopus affiliation id (AFID) to a Cristin institution and unit. {@code
 * cristinUnitId} is always set: {@code {institution}.0.0.0} for the top level, otherwise the
 * sub-unit, and it always belongs to {@code cristinInstitutionId}. {@code scopusName}, {@code
 * mappingCount} and {@code totalMappingCount} are null when the item does not have them.
 */
public record ScopusAffiliationMapping(
    String scopusAffiliationId,
    String cristinInstitutionId,
    String cristinUnitId,
    String scopusName,
    Integer mappingCount,
    Integer totalMappingCount,
    boolean needsReview,
    ScopusMappingSource source,
    Instant updatedAt,
    String updatedBy,
    int revision,
    boolean deleted) {

  private static final Pattern CRISTIN_UNIT_ID_PATTERN =
      Pattern.compile("^(\\d+)\\.\\d+\\.\\d+\\.\\d+$");

  public ScopusAffiliationMapping {
    requireNumericId(scopusAffiliationId, "scopusAffiliationId");
    requireNumericId(cristinInstitutionId, "cristinInstitutionId");
    requireNonNull(cristinUnitId, "cristinUnitId");
    requireNonNull(source, "source");
    requireNonNull(updatedAt, "updatedAt");
    requireNonNull(updatedBy, "updatedBy");
    if (!isUnitOfInstitution(cristinUnitId, cristinInstitutionId)) {
      throw new IllegalArgumentException(
          "Cristin unit %s of Scopus affiliation %s is not a unit of institution %s"
              .formatted(cristinUnitId, scopusAffiliationId, cristinInstitutionId));
    }
  }

  private static boolean isUnitOfInstitution(String cristinUnitId, String cristinInstitutionId) {
    var unitIdMatcher = CRISTIN_UNIT_ID_PATTERN.matcher(cristinUnitId);
    return unitIdMatcher.matches() && unitIdMatcher.group(1).equals(cristinInstitutionId);
  }
}
