package no.unit.nva.transformer.dto;

import static java.util.Objects.isNull;

import org.datacite.schema.kernel_4.Resource.Identifier;

public class IdentifierDto {

  private final String value;
  private final IdentifierType type;

  public IdentifierDto(String value, IdentifierType type) {
    this.value = value;
    this.type = type;
  }

  private IdentifierDto(Builder builder) {
    this(builder.value, builder.type);
  }

  /**
   * Creates a Datacite Identifier representation of the object.
   *
   * @return A Datacite Identifier.
   */
  public Identifier asIdentifier() {
    Identifier identifier = new Identifier();
    identifier.setIdentifierType(type.getValue());
    identifier.setValue(value);
    return identifier;
  }

  public static final class Builder {

    private String value;
    private IdentifierType type;

    public Builder() {}

    public Builder withValue(String value) {
      this.value = value;
      return this;
    }

    public Builder withType(IdentifierType type) {
      this.type = type;
      return this;
    }

    public IdentifierDto build() {
      if (isNull(type)) {
        type = IdentifierType.URL;
      }
      if (isNull(value)) {
        throw new IllegalArgumentException("Identifier cannot be built with null value");
      }
      return new IdentifierDto(this);
    }
  }
}
