package no.unit.nva.publication.model.business.importcandidate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.Username;
import nva.commons.core.JacocoGenerated;

import java.time.Instant;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class NotApplicable implements ImportStatus {

    private final static String SET_TO_NOT_APPLICABLE_BY = "setToNotApplicableBy";
    private final static String COMMENT = "comment";
    private final static String SET_TO_NOT_APPLICABLE_DATE = "setToNotApplicableDate";

    @JsonProperty(SET_TO_NOT_APPLICABLE_BY)
    private final Username setToNotApplicableBy;

    @JsonProperty(COMMENT)
    private final String comment;

    @JsonProperty(SET_TO_NOT_APPLICABLE_DATE)
    private final Instant setToNotApplicableDate;

    public NotApplicable(@JsonProperty(SET_TO_NOT_APPLICABLE_BY) Username setToNotApplicableBy,
                         @JsonProperty(COMMENT) String comment,
                         @JsonProperty(SET_TO_NOT_APPLICABLE_DATE) Instant setToNotApplicableDate) {
        this.setToNotApplicableBy = setToNotApplicableBy;
        this.comment = comment;
        this.setToNotApplicableDate = setToNotApplicableDate;
    }

    @JacocoGenerated
    public Username getSetToNotApplicableBy() {
        return setToNotApplicableBy;
    }

    @JacocoGenerated
    public String getComment() {
        return comment;
    }

    @JacocoGenerated
    public Instant getSetToNotApplicableDate() {
        return setToNotApplicableDate;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NotApplicable)) {
            return false;
        }
        NotApplicable that = (NotApplicable) o;
        return Objects.equals(getSetToNotApplicableBy(), that.getSetToNotApplicableBy())
                && Objects.equals(getComment(), that.getComment())
                && Objects.equals(getSetToNotApplicableDate(), that.getSetToNotApplicableDate());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getSetToNotApplicableBy(),
                getComment(),
                getSetToNotApplicableDate());
    }
}
