package no.unit.nva.publication.scopusmapping;

import static java.util.Objects.requireNonNull;

/**
 * Where a curator corrected an author mapping: the Scopus publication, and the author's sequence
 * number in it.
 */
public record ScopusCorrectionContext(String scopusPublicationId, int sequenceNumber) {

  public ScopusCorrectionContext {
    requireNonNull(scopusPublicationId, "scopusPublicationId");
  }
}
