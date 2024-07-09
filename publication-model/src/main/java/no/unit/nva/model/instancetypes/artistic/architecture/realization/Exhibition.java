package no.unit.nva.model.instancetypes.artistic.architecture.realization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.instancetypes.artistic.architecture.ArchitectureOutput;
import no.unit.nva.model.time.Period;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Exhibition implements ArchitectureOutput {
    public static final String NAME = "name";
    public static final String PLACE = "place";
    public static final String ORGANIZER = "organizer";
    public static final String DATE = "date";
    public static final String OTHER_INFORMATION = "otherInformation";
    @JsonProperty(NAME)
    private final String name;
    @JsonProperty(PLACE)
    private final UnconfirmedPlace place;
    @JsonProperty(ORGANIZER)
    private final String organizer;
    @JsonProperty(DATE)
    private final Period date;
    @JsonProperty(OTHER_INFORMATION)
    private final String otherInformation;
    @JsonProperty(SEQUENCE_FIELD)
    private final int sequence;

    public Exhibition(@JsonProperty(NAME) String name,
                      @JsonProperty(PLACE) UnconfirmedPlace place,
                      @JsonProperty(ORGANIZER) String organizer,
                      @JsonProperty(DATE) Period date,
                      @JsonProperty(OTHER_INFORMATION) String otherInformation,
                      @JsonProperty(SEQUENCE_FIELD) int sequence) {
        this.name = name;
        this.place = place;
        this.organizer = organizer;
        this.date = date;
        this.otherInformation = otherInformation;
        this.sequence = sequence;
    }

    public String getName() {
        return name;
    }

    public UnconfirmedPlace getPlace() {
        return place;
    }

    public String getOrganizer() {
        return organizer;
    }

    public Period getDate() {
        return date;
    }

    public String getOtherInformation() {
        return otherInformation;
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
        if (!(o instanceof Exhibition)) {
            return false;
        }
        Exhibition that = (Exhibition) o;
        return getSequence() == that.getSequence()
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getPlace(), that.getPlace())
                && Objects.equals(getOrganizer(), that.getOrganizer())
                && Objects.equals(getDate(), that.getDate())
                && Objects.equals(getOtherInformation(), that.getOtherInformation());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getPlace(), getOrganizer(), getDate(), getOtherInformation(), getSequence());
    }
}
