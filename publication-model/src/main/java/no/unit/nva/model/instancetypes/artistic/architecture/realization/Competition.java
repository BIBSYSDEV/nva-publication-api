package no.unit.nva.model.instancetypes.artistic.architecture.realization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.artistic.architecture.ArchitectureOutput;
import no.unit.nva.model.time.Time;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Competition implements ArchitectureOutput {

    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String DATE = "date";

    @JsonProperty(NAME)
    private final String name;
    @JsonProperty(DESCRIPTION)
    private final String description;
    @JsonProperty(DATE)
    private final Time date;
    @JsonProperty(SEQUENCE_FIELD)
    private final int sequence;

    public Competition(@JsonProperty(NAME) String name,
                       @JsonProperty(DESCRIPTION) String description,
                       @JsonProperty(DATE) Time date,
                       @JsonProperty(SEQUENCE_FIELD) int sequence) {
        this.name = name;
        this.description = description;
        this.date = date;
        this.sequence = sequence;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Time getDate() {
        return date;
    }

    @Override
    public int getSequence() {
        return sequence;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Competition)) {
            return false;
        }
        Competition that = (Competition) o;
        return getSequence() == that.getSequence()
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getDescription(), that.getDescription())
                && Objects.equals(getDate(), that.getDate());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDescription(), getDate(), getSequence());
    }
}
