package no.unit.nva.transformer.dto;

import static java.util.Objects.nonNull;

import org.datacite.schema.kernel_4.Resource;
import org.datacite.schema.kernel_4.ResourceType;

public class ResourceTypeDto {
  private final ResourceType resourceTypeGeneral;
  private final String value;

  /**
   * Simplistically maps String values to Datacite resource types.
   *
   * @param value A string representing a type.
   */
  public ResourceTypeDto(String value) {
    this.value = value;
    if (nonNull(value)) {
      this.resourceTypeGeneral = ResourceType.TEXT;
    } else {
      this.resourceTypeGeneral = ResourceType.OTHER;
    }
  }

  private ResourceTypeDto(Builder builder) {
    this(builder.value);
  }

  /**
   * Creates a Datacite Resource type from the object.
   *
   * @return A Datacite ResourceType.
   */
  public Resource.ResourceType toResourceType() {
    Resource.ResourceType resourceType = new Resource.ResourceType();
    resourceType.setValue(value);
    resourceType.setResourceTypeGeneral(resourceTypeGeneral);
    return resourceType;
  }

  public static final class Builder {
    private String value;

    public Builder() {}

    public Builder withValue(String value) {
      this.value = value;
      return this;
    }

    public ResourceTypeDto build() {
      return new ResourceTypeDto(this);
    }
  }
}
