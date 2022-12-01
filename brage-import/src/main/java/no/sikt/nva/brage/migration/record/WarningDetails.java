package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class WarningDetails {

    private final Warning warningCode;
    private List<String> details;

    public WarningDetails(@JsonProperty("warningCode") Warning warningCode,
                          @JsonProperty("details")List<String> details) {
        this.warningCode = warningCode;
        this.details = details;
    }

    public WarningDetails(Warning warningCode) {
        this.warningCode = warningCode;
    }

    public WarningDetails(Warning warningCode, String detail) {
        this.warningCode = warningCode;
        this.details = Collections.singletonList(detail);
    }

    @JsonProperty("warningCode")
    public Warning getWarningCode() {
        return warningCode;
    }

    @JsonProperty("details")
    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(warningCode);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WarningDetails)) {
            return false;
        }
        var warningDetail = (WarningDetails) o;
        return this.warningCode.equals(warningDetail.warningCode);
    }

    @Override
    public String toString() {
        return warningCode + " = " + details;
    }

    public enum Warning {
        SUBJECT_WARNING,
        LANGUAGE_MAPPED_TO_UNDEFINED,
        MULTIPLE_DESCRIPTION_PRESENT,
        VOLUME_NOT_NUMBER_WARNING,
        ISSUE_NOT_NUMBER_WARNING,
        MULTIPLE_UNMAPPABLE_TYPES,
        INVALID_CC_LICENSE,
        PAGE_NUMBER_FORMAT_NOT_RECOGNIZED,
        MULTIPLE_ISBN_VALUES_WARNING,
        EMPTY_OR_NONEXISTENT_COLLECTION
    }
}
