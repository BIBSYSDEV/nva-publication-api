package no.unit.nva.publication.events.handlers.batch;

public enum Comparator {
  MATCHES,
  CONTAINS;

  public boolean matches(String actual, String expected) {
    return switch (this) {
      case CONTAINS -> actual.contains(expected);
      case MATCHES -> actual.equals(expected);
    };
  }
}
