package no.unit.nva.publication.scopusmapping.domain;

import static java.util.Objects.requireNonNull;

/**
 * Where a curator corrected an author mapping: the Scopus publication, and the author's sequence
 * number in it.
 */
public record ScopusCorrectionContext(String scopusPublicationIdentifier, int sequenceNumber) {

  public ScopusCorrectionContext {
    requireNonNull(scopusPublicationIdentifier, "scopusPublicationIdentifier");
  }
}
