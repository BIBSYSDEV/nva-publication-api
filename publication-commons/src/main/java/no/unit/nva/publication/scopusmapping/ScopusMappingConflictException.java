package no.unit.nva.publication.scopusmapping;

import nva.commons.apigateway.exceptions.ConflictException;

/**
 * Thrown when a mapping does not have the revision the caller expected, so nothing was written. As
 * a {@link ConflictException}, API handlers answer it with 409 Conflict.
 */
public class ScopusMappingConflictException extends ConflictException {

  public ScopusMappingConflictException(String message) {
    super(message);
  }
}
