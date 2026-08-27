package no.unit.nva.transformer.dto;

import static java.util.Objects.isNull;

import org.datacite.schema.kernel_4.Resource.AlternateIdentifiers.AlternateIdentifier;

public class AlternateIdentifierDto {

  private static final String type = "URL";

  private final String value;

  private AlternateIdentifierDto(String value) {
    this.value = value;
  }

  public AlternateIdentifierDto(Builder builder) {
    this(builder.value);
  }

  /**
   * Creates a Datacite AlternateIdentifier representation of the object.
   *
   * @return A Datacite AlternateIdentifier.
   */
  public AlternateIdentifier asAlternateIdentifier() {
    AlternateIdentifier identifier = new AlternateIdentifier();
    identifier.setAlternateIdentifierType(type);
    identifier.setValue(value);
    return identifier;
  }

  public static final class Builder {

    private String value;

    public Builder() {}

    public AlternateIdentifierDto.Builder withValue(String value) {
      this.value = value;
      return this;
    }

    public AlternateIdentifierDto build() {
      if (isNull(value)) {
        throw new IllegalArgumentException("Value is required");
      }
      return new AlternateIdentifierDto(this);
    }
  }
}
