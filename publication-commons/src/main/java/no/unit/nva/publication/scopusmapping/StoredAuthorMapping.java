package no.unit.nva.publication.scopusmapping;

import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toAuthorMapping;

import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * The stored {@code CURRENT} item of an author mapping, as the write path sees it, with what a new
 * version takes over from it. The revision is {@link #NO_REVISION} when there is no item.
 */
record StoredAuthorMapping(
    Optional<ScopusAuthorMapping> mapping, int revision, String scopusName, String orcid) {

  static final int NO_REVISION = 0;
  static final StoredAuthorMapping NONE =
      new StoredAuthorMapping(Optional.empty(), NO_REVISION, null, null);

  static StoredAuthorMapping fromItem(Map<String, AttributeValue> item) {
    var storedMapping = toAuthorMapping(item);
    return new StoredAuthorMapping(
        Optional.of(storedMapping),
        storedMapping.revision(),
        storedMapping.scopusName(),
        storedMapping.orcid());
  }

  /** An empty expected revision means the caller saw no mapping: there must be no live one. */
  boolean hasExpectedRevision(Optional<Integer> expectedRevision) {
    return expectedRevision
        .map(expected -> expected == revision)
        .orElseGet(() -> mapping.filter(storedMapping -> !storedMapping.deleted()).isEmpty());
  }
}
