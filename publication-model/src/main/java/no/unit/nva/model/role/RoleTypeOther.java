package no.unit.nva.model.role;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class RoleTypeOther extends RoleType {

    @JsonProperty(DESCRIPTION_FIELD)
    private final String description;

    public RoleTypeOther(@JsonProperty(TYPE_FIELD) Role type,
                         @JsonProperty(DESCRIPTION_FIELD) String description) {
        super(type);
        this.description = description;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getDescription());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RoleTypeOther that = (RoleTypeOther) o;
        return Objects.equals(getDescription(), that.getDescription());
    }

    public String getDescription() {
        return description;
    }
}
