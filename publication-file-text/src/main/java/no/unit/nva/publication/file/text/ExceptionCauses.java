package no.unit.nva.publication.file.text;

import static java.util.Objects.nonNull;

/** Utilities for inspecting exception cause chains. */
final class ExceptionCauses {

  private ExceptionCauses() {
    // NO-OP
  }

  /**
   * Returns {@code true} if any throwable in the cause chain of {@code exception} is an instance of
   * any of the given {@code types}.
   */
  static boolean hasCauseOfType(Throwable exception, Class<?>... types) {
    for (Throwable current = exception; nonNull(current); current = current.getCause()) {
      for (var type : types) {
        if (type.isInstance(current)) {
          return true;
        }
      }
    }
    return false;
  }
}
