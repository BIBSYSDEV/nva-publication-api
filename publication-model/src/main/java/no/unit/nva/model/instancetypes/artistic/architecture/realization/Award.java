package no.unit.nva.model.instancetypes.artistic.architecture.realization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.artistic.architecture.ArchitectureOutput;
import no.unit.nva.model.time.Instant;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Award implements ArchitectureOutput {

    public static final String NAME = "name";
    public static final String ORGANIZER = "organizer";
    public static final String DATE = "date";
    public static final String RANKING = "ranking";
    public static final String OTHER_INFORMATION = "otherInformation";

    @JsonProperty(NAME)
    private final String name;
    @JsonProperty(ORGANIZER)
    private final String organizer;
    @JsonProperty(DATE)
    private final Instant date;
    @JsonProperty(RANKING)
    private final Integer ranking;
    @JsonProperty(OTHER_INFORMATION)
    private final String otherInformation;
    @JsonProperty(SEQUENCE_FIELD)
    private final int sequence;

    public Award(@JsonProperty(NAME) String name,
                 @JsonProperty(ORGANIZER) String organizer,
                 @JsonProperty(DATE) Instant date,
                 @JsonProperty(RANKING) Integer ranking,
                 @JsonProperty(OTHER_INFORMATION) String otherInformation,
                 @JsonProperty(SEQUENCE_FIELD) int sequence) {
        this.name = name;
        this.organizer = organizer;
        this.date = date;
        this.ranking = ranking;
        this.otherInformation = otherInformation;
        this.sequence = sequence;
    }

    public String getName() {
        return name;
    }

    public String getOrganizer() {
        return organizer;
    }

    public Instant getDate() {
        return date;
    }

    public Integer getRanking() {
        return ranking;
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
        if (!(o instanceof Award)) {
            return false;
        }
        Award award = (Award) o;
        return getSequence() == award.getSequence()
                && Objects.equals(getName(), award.getName())
                && Objects.equals(getOrganizer(), award.getOrganizer())
                && Objects.equals(getDate(), award.getDate())
                && Objects.equals(getRanking(), award.getRanking())
                && Objects.equals(getOtherInformation(), award.getOtherInformation());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getOrganizer(), getDate(), getRanking(), getOtherInformation(), getSequence());
    }
}
