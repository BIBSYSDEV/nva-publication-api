package no.unit.nva.transformer.dto;

public class PublisherDto {
  private final String value;

  @SuppressWarnings("PMD.UnusedPrivateField")
  private final String lang;

  public PublisherDto(String value, String lang) {
    this.value = value;
    this.lang = lang;
  }

  private PublisherDto(Builder builder) {
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

    public PublisherDto build() {
      return new PublisherDto(this);
    }
  }
}
