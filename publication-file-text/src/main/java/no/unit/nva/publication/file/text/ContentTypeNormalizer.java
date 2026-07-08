package no.unit.nva.publication.file.text;

import static java.util.Objects.isNull;

import java.util.Locale;

/**
 * Normalizes stored Content-Type values into the canonical form extractors match against: the bare
 * media type, lowercased, with any RFC 2045 parameters (such as {@code charset}) removed. A null
 * content type normalizes to the empty string, so callers never dispatch on null.
 */
final class ContentTypeNormalizer {

  private static final String NO_CONTENT_TYPE = "";
  private static final String PARAMETER_SEPARATOR = ";";
  private static final int MEDIA_TYPE_PART = 0;
  private static final int MEDIA_TYPE_AND_PARAMETERS = 2;

  private ContentTypeNormalizer() {
    // NO-OP
  }

  static String normalize(String contentType) {
    if (isNull(contentType)) {
      return NO_CONTENT_TYPE;
    }
    var mediaType =
        contentType.split(PARAMETER_SEPARATOR, MEDIA_TYPE_AND_PARAMETERS)[MEDIA_TYPE_PART];
    return mediaType.strip().toLowerCase(Locale.ROOT);
  }
}
