package no.unit.nva.transformer.dto;

import org.datacite.schema.kernel_4.Resource.Creators.Creator;
import org.datacite.schema.kernel_4.Resource.Creators.Creator.CreatorName;

public final class CreatorDto {
  public static final String SEPARATOR = ", ";
  public static final int COMMA_SEPARATED_NAME = 2;
  private final String creatorName;
  private String givenName;
  private String familyName;

  private CreatorDto(Builder builder) {
    this.creatorName = builder.creatorName;
    setGivenAndFamilyName();
  }

  private void setGivenAndFamilyName() {
    var names = creatorName.split(SEPARATOR);
    if (names.length == COMMA_SEPARATED_NAME) {
      familyName = names[0];
      givenName = names[1];
    }
  }

  /**
   * Creates a Datacite Creator from the CreatorDto.
   *
   * @return Datacite Creator.
   */
  public Creator toCreator() {
    Creator creator = new Creator();
    CreatorName creatorNameO = new CreatorName();
    creatorNameO.setValue(creatorName);
    creator.setCreatorName(creatorNameO);
    creator.setFamilyName(familyName);
    creator.setGivenName(givenName);
    return creator;
  }

  public static final class Builder {
    private String creatorName;

    public Builder() {}

    public Builder withCreatorName(String creatorName) {
      this.creatorName = creatorName;
      return this;
    }

    public CreatorDto build() {
      return new CreatorDto(this);
    }
  }
}
