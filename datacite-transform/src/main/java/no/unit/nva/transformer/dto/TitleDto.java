package no.unit.nva.transformer.dto;

public final class TitleDto {
  private final String value;

  private TitleDto(Builder builder) {
    this.value = builder.value;
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
