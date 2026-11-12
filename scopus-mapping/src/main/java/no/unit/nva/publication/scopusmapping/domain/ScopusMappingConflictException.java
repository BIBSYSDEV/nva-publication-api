package no.unit.nva.publication.scopusmapping.domain;

/**
 * Thrown when a mapping is not at the revision the caller expected, so nothing was written. The
 * caller should read the mapping again and decide what to do.
 */
public class ScopusMappingConflictException extends Exception {

  public ScopusMappingConflictException(String message) {
    super(message);
  }
}
