package no.unit.nva.model.role;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

public class RoleType {

  public static final String TYPE_FIELD = "type";
  public static final String DESCRIPTION_FIELD = "description";

  @JsonProperty(TYPE_FIELD)
  private final Role type;

  public RoleType(Role type) {
    this.type = type;
  }

  @JsonCreator
  public static RoleType fromJson(
      @JsonProperty(TYPE_FIELD) Role type, @JsonProperty(DESCRIPTION_FIELD) String description) {
    return Role.OTHER.equals(type) && StringUtils.isNotBlank(description)
        ? new RoleTypeOther(Role.OTHER, description)
        : new RoleType(type);
  }

  @JacocoGenerated
  @Override
  public int hashCode() {
    return Objects.hash(getType());
  }

  @JacocoGenerated
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RoleType roleType)) {
      return false;
    }
    return getType() == roleType.getType();
  }

  public Role getType() {
    return type;
  }
}
