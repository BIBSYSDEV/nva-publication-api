package no.unit.nva.publication.scopusmapping;

import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.revisionOf;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toAuthorMappingOrLog;

import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * The stored {@code CURRENT} item of an author mapping, as the write path sees it. A malformed item
 * counts as no mapping, like a deleted one, so a curator correction replaces it; its revision still
 * guards that write.
 */
record StoredAuthorMapping(Optional<ScopusAuthorMapping> mapping, Optional<Integer> revision) {

  static final StoredAuthorMapping NONE =
      new StoredAuthorMapping(Optional.empty(), Optional.empty());

  static StoredAuthorMapping fromItem(Map<String, AttributeValue> item)
      throws ScopusMappingConflictException {
    return new StoredAuthorMapping(toAuthorMappingOrLog(item), Optional.of(readableRevision(item)));
  }

  /** An empty expected revision means the caller saw no mapping: there must be no live one. */
  boolean hasExpectedRevision(Optional<Integer> expectedRevision) {
    return expectedRevision.isPresent()
        ? revision.equals(expectedRevision)
        : liveMapping().isEmpty();
  }

  Optional<ScopusAuthorMapping> liveMappingTo(String cristinPersonId) {
    return liveMapping()
        .filter(storedMapping -> storedMapping.cristinPersonId().equals(cristinPersonId));
  }

  private Optional<ScopusAuthorMapping> liveMapping() {
    return mapping.filter(storedMapping -> !storedMapping.deleted());
  }

  private static int readableRevision(Map<String, AttributeValue> item)
      throws ScopusMappingConflictException {
    try {
      return revisionOf(item);
    } catch (IllegalArgumentException exception) {
      throw new ScopusMappingConflictException(
          "Malformed Scopus author mapping can't be replaced: " + exception.getMessage(),
          exception);
    }
  }
}
