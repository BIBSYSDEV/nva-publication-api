package no.unit.nva.model.instancetypes.exhibition.manifestations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.nva.model.time.Instant;
import no.unit.nva.model.time.Time;
import nva.commons.core.JacocoGenerated;

public class ExhibitionMentionInPublication implements ExhibitionProductionManifestation {

    public static final String TITLE_FIELD = "title";
    public static final String ISSUE_FIELD = "issue";
    public static final String DATE_FIELD = "date";
    public static final String OTHER_INFORMATION_FIELD = "otherInformation";
    public static final String PAGES_FIELD = "pages";

    @JsonProperty(TITLE_FIELD)
    private final String title;
    @JsonProperty(ISSUE_FIELD)
    private final String issue;
    @JsonProperty(DATE_FIELD)
    private final Time date;
    @JsonProperty(OTHER_INFORMATION_FIELD)
    private final String otherInformation;
    @JsonProperty(PAGES_FIELD)
    private final String pages;

    @JsonCreator
    public ExhibitionMentionInPublication(@JsonProperty(TITLE_FIELD) String title,
                                          @JsonProperty(ISSUE_FIELD) String issue,
                                          @JsonProperty(PAGES_FIELD) String pages,
                                          @JsonProperty(DATE_FIELD) Instant date,
                                          @JsonProperty(OTHER_INFORMATION_FIELD) String otherInformation) {
        this.title = title;
        this.issue = issue;
        this.pages = pages;
        this.date = date;
        this.otherInformation = otherInformation;
    }

    public String getPages() {
        return pages;
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

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getTitle(), getIssue(), getDate(), getOtherInformation(), getPages());
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
        ExhibitionMentionInPublication that = (ExhibitionMentionInPublication) o;
        return Objects.equals(getTitle(), that.getTitle())
               && Objects.equals(getIssue(), that.getIssue())
               && Objects.equals(getDate(), that.getDate())
               && Objects.equals(getOtherInformation(), that.getOtherInformation())
               && Objects.equals(getPages(), that.getPages());
    }
}
