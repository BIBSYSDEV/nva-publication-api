package no.unit.nva.transformer.dto;

public class TitleDto {
  private final String value;

  @SuppressWarnings("PMD.UnusedPrivateField")
  private final String type;

  public TitleDto(String value, String type) {
    this.value = value;
    this.type = type;
  }

  private TitleDto(Builder builder) {
    this(builder.value, null);
  }

  public String getValue() {
    return value;
  }

  public static final class Builder {
    private String value;

    public Builder() {}

    public Builder withValue(String value) {
      this.value = value;
      return this;
    }

    public TitleDto build() {
      return new TitleDto(this);
    }
  }
}
