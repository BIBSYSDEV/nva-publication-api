package no.unit.nva.publication.model.business.importcandidate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.Username;
import nva.commons.core.JacocoGenerated;

import java.time.Instant;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class NotApplicable implements ImportStatus {

    private final static String SET_TO_NOT_APPLICABLE_BY = "setBy";
    private final static String COMMENT = "comment";
    private final static String SET_TO_NOT_APPLICABLE_DATE = "setToDate";

    @JsonProperty(SET_TO_NOT_APPLICABLE_BY)
    private final Username setBy;

    @JsonProperty(COMMENT)
    private final String comment;

    @JsonProperty(SET_TO_NOT_APPLICABLE_DATE)
    private final Instant setToDate;

    @JsonCreator
    public NotApplicable(@JsonProperty(SET_TO_NOT_APPLICABLE_BY) Username setToNotApplicableBy,
                         @JsonProperty(COMMENT) String comment,
                         @JsonProperty(SET_TO_NOT_APPLICABLE_DATE) Instant setToNotApplicableDate) {
        this.setBy = setToNotApplicableBy;
        this.comment = comment;
        this.setToDate = setToNotApplicableDate;
    }

    @JacocoGenerated
    public Username getSetBy() {
        return setBy;
    }

    @JacocoGenerated
    public String getComment() {
        return comment;
    }

    @JacocoGenerated
    public Instant getSetToDate() {
        return setToDate;
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
        return Objects.equals(getSetBy(), that.getSetBy())
                && Objects.equals(getComment(), that.getComment())
                && Objects.equals(getSetToDate(), that.getSetToDate());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getSetBy(),
                getComment(),
                getSetToDate());
    }
}
