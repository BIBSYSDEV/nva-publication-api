package no.unit.nva.publication.scopusmapping.domain;

import static java.util.Objects.requireNonNull;

import java.time.Instant;

/**
 * One written version of a mapping: who wrote it and when. The revision is 1 for the first version
 * and one more for each version after it. A deletion is a version too, written with {@code deleted}
 * set, and a deleted mapping counts as no mapping.
 */
public record ScopusMappingVersion(
    ScopusMappingSource source,
    Instant updatedAt,
    String updatedBy,
    int revision,
    boolean deleted) {

  public static final int FIRST_REVISION = 1;

  public ScopusMappingVersion {
    requireNonNull(source, "source");
    requireNonNull(updatedAt, "updatedAt");
    requireNonNull(updatedBy, "updatedBy");
    if (revision < FIRST_REVISION) {
      throw new IllegalArgumentException(
          "revision must be at least %d: %d".formatted(FIRST_REVISION, revision));
    }
  }
}
