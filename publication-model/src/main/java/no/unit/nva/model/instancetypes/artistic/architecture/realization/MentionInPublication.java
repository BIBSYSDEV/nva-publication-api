package no.unit.nva.model.instancetypes.artistic.architecture.realization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.artistic.architecture.ArchitectureOutput;
import no.unit.nva.model.time.Instant;
import no.unit.nva.model.time.Time;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class MentionInPublication implements ArchitectureOutput {

    public static final String TITLE = "title";
    public static final String ISSUE = "issue";
    public static final String DATE = "date";
    public static final String OTHER_INFORMATION = "otherInformation";

    @JsonProperty(TITLE)
    private final String title;
    @JsonProperty(ISSUE)
    private final String issue;
    @JsonProperty(DATE)
    private final Time date;
    @JsonProperty(OTHER_INFORMATION)
    private final String otherInformation;
    @JsonProperty(SEQUENCE_FIELD)
    private final int sequence;

    public MentionInPublication(@JsonProperty(TITLE) String title,
                                @JsonProperty(ISSUE) String issue,
                                @JsonProperty(DATE) Instant date,
                                @JsonProperty(OTHER_INFORMATION) String otherInformation,
                                @JsonProperty(SEQUENCE_FIELD) int sequence) {
        this.title = title;
        this.issue = issue;
        this.date = date;
        this.otherInformation = otherInformation;
        this.sequence = sequence;
    }

    public String getTitle() {
        return title;
    }

    public String getIssue() {
        return issue;
    }

    public Time getDate() {
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
        if (!(o instanceof MentionInPublication)) {
            return false;
        }
        MentionInPublication that = (MentionInPublication) o;
        return getSequence() == that.getSequence()
                && Objects.equals(getTitle(), that.getTitle())
                && Objects.equals(getIssue(), that.getIssue())
                && Objects.equals(getDate(), that.getDate())
                && Objects.equals(getOtherInformation(), that.getOtherInformation());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getTitle(), getIssue(), getDate(), getOtherInformation(), getSequence());
    }
}
