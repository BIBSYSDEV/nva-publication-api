package no.unit.nva.publication.file.text;

import static java.util.Objects.isNull;

/** Sanitizes externally-sourced strings before they are written to log output. */
final class LogSanitizer {

  private static final int MAX_LENGTH = 256;
  private static final String LOG_INJECTION_CHARS = "[\\r\\n\\t]";
  private static final String LOG_INJECTION_REPLACEMENT = "_";
  private static final String EMPTY_STRING = "";

  private LogSanitizer() {
    // NO-OP
  }

  static String sanitize(String value) {
    if (isNull(value)) {
      return EMPTY_STRING;
    }
    var truncated = value.length() > MAX_LENGTH ? value.substring(0, MAX_LENGTH) : value;
    return truncated.replaceAll(LOG_INJECTION_CHARS, LOG_INJECTION_REPLACEMENT);
  }
}
