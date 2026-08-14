package no.unit.nva.transformer.dto;

public enum IdentifierType {
  DOI("DOI"),
  URL("URL");

  private final String value;

  IdentifierType(String type) {
    this.value = type;
  }

  public static IdentifierType fromValue(String v) {
    for (IdentifierType c : values()) {
      if (c.getValue().equalsIgnoreCase(v)) {
        return c;
      }
    }
    return null;
  }

  public String getValue() {
    return value;
  }
}
