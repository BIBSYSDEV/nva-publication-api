package no.unit.nva.publication.scopusmapping;

import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.ORCID;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.SCOPUS_NAME;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.parseOrLogAndSkip;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.revisionOf;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.textOf;

import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * The stored {@code CURRENT} item of an author mapping, as the write path sees it, with what a new
 * version takes over from it. The revision is {@link #NO_REVISION} when there is no item. A
 * malformed item counts as no mapping, like a deleted one, so a curator correction replaces it. Its
 * revision still guards that write, and its Scopus name and ORCID are kept.
 */
record StoredAuthorMapping(
    Optional<ScopusAuthorMapping> mapping, int revision, String scopusName, String orcid) {

  static final int NO_REVISION = 0;
  static final StoredAuthorMapping NONE =
      new StoredAuthorMapping(Optional.empty(), NO_REVISION, null, null);

  static StoredAuthorMapping fromItem(Map<String, AttributeValue> item) {
    return new StoredAuthorMapping(
        parseOrLogAndSkip(item, ScopusMappingItems::toAuthorMapping),
        readableRevision(item),
        textOf(item, SCOPUS_NAME),
        textOf(item, ORCID));
  }

  /**
   * Without a readable revision there is nothing to condition the replacing write on. That is a
   * fault in the stored data, not in the caller's request, and no retry can fix it.
   */
  private static int readableRevision(Map<String, AttributeValue> item) {
    try {
      return revisionOf(item);
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException(
          "Malformed Scopus author mapping can't be replaced: " + exception.getMessage(),
          exception);
    }
  }

  /** An empty expected revision means the caller saw no mapping: there must be no live one. */
  boolean hasExpectedRevision(Optional<Integer> expectedRevision) {
    return expectedRevision
        .map(expected -> expected == revision)
        .orElseGet(() -> mapping.filter(storedMapping -> !storedMapping.deleted()).isEmpty());
  }
}
