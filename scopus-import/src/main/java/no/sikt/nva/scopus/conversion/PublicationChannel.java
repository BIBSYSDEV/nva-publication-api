package no.sikt.nva.scopus.conversion;

public enum PublicationChannel {
  SERIAL_PUBLICATION("serial-publication"),
  PUBLISHER("publisher");

  private final String value;

  PublicationChannel(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
