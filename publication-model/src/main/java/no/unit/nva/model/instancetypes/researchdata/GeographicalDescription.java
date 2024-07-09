package no.unit.nva.model.instancetypes.researchdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class GeographicalDescription {
    public static final String DESCRIPTION_FIELD = "description";
    @JsonProperty(DESCRIPTION_FIELD)
    private final String description;

    public GeographicalDescription(@JsonProperty(DESCRIPTION_FIELD) String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GeographicalDescription)) {
            return false;
        }
        GeographicalDescription that = (GeographicalDescription) o;
        return Objects.equals(getDescription(), that.getDescription());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getDescription());
    }
}
