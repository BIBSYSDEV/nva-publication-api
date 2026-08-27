package no.unit.nva.publication.file.text;

import java.io.IOException;

/**
 * Signals that a stored object exceeds the maximum size the extraction pipeline accepts. Thrown
 * before any bytes are transferred, so no local storage has been consumed. Carries the object's
 * declared size, the limit it exceeded, and the ETag of the object version whose size was
 * inspected, so callers can record a durable, version-specific marker instead of retrying.
 */
public final class FileTooLargeException extends IOException {

  private static final String MESSAGE_TEMPLATE = "Object is %d bytes, exceeding the %d byte limit";

  private final long objectSizeBytes;
  private final long limitBytes;
  private final String etag;

  public FileTooLargeException(long objectSizeBytes, long limitBytes, String etag) {
    super(MESSAGE_TEMPLATE.formatted(objectSizeBytes, limitBytes));
    this.objectSizeBytes = objectSizeBytes;
    this.limitBytes = limitBytes;
    this.etag = etag;
  }

  public long getObjectSizeBytes() {
    return objectSizeBytes;
  }

  public long getLimitBytes() {
    return limitBytes;
  }

  public String getEtag() {
    return etag;
  }
}
