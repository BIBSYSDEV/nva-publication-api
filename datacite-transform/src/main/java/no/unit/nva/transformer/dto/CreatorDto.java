package no.unit.nva.transformer.dto;

import java.util.List;
import org.datacite.schema.kernel_4.Resource.Creators.Creator;
import org.datacite.schema.kernel_4.Resource.Creators.Creator.CreatorName;

public class CreatorDto {
  public static final String SEPARATOR = ", ";
  public static final int COMMA_SEPARATED_NAME = 2;
  private final String creatorName;
  private String givenName;
  private String familyName;

  @SuppressWarnings("PMD.UnusedPrivateField")
  private final List<Object> nameIdentifier;

  @SuppressWarnings("PMD.UnusedPrivateField")
  private final List<Object> affiliation;

  /**
   * Creates a representation of the DataCiteMetadataDto Creator object that can be transformed to
   * Datacite Creator.
   *
   * @param creatorName A name string.
   * @param nameIdentifier Not yet in use.
   * @param affiliation Not yet in use.
   */
  public CreatorDto(String creatorName, List<Object> nameIdentifier, List<Object> affiliation) {
    this.creatorName = creatorName;
    this.nameIdentifier = nameIdentifier;
    this.affiliation = affiliation;

    setGivenAndFamilyName();
  }

  private CreatorDto(Builder builder) {
    this(builder.creatorName, null, null);
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
