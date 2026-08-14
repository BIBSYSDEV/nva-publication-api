package no.unit.nva.transformer.dto;

public final class PublisherDto {
  private final String value;

  private PublisherDto(Builder builder) {
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

    public PublisherDto build() {
      return new PublisherDto(this);
    }
  }
}
