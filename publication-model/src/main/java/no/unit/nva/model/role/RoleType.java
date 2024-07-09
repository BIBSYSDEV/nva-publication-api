package no.unit.nva.model.role;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class RoleType {

    public static final String TYPE_FIELD = "type";
    public static final String DESCRIPTION_FIELD = "description";
    @JsonProperty(TYPE_FIELD)
    private final Role type;

    @JsonCreator
    public RoleType(@JsonProperty(TYPE_FIELD) Role type) {
        this.type = type;
    }

    public RoleType createOther(@JsonProperty(DESCRIPTION_FIELD) String description) {
        return new RoleTypeOther(Role.OTHER, description);
    }

    @JsonCreator
    public RoleType fromJson(@JsonProperty(TYPE_FIELD) Role type,
                             @JsonProperty(DESCRIPTION_FIELD) String description) {
        return Role.OTHER.equals(type) ? createOther(description) : new RoleType(type);
    }

    @Deprecated
    @JsonCreator
    public RoleType create(Role type) {
        return new RoleType(type);
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
        if (!(o instanceof RoleType)) {
            return false;
        }
        RoleType roleType = (RoleType) o;
        return getType() == roleType.getType();
    }

    public Role getType() {
        return type;
    }
}
