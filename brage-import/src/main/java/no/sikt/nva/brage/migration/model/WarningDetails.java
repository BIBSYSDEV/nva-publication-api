package no.sikt.nva.brage.migration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class WarningDetails {

    private final Warning warningCode;
    private List<String> details;

    @JacocoGenerated
    public WarningDetails(Warning warningCode, List<String> details) {
        this.warningCode = warningCode;
        this.details = details;
    }

    @JacocoGenerated
    public WarningDetails(Warning warningCode) {
        this.warningCode = warningCode;
    }

    @JacocoGenerated
    public WarningDetails(Warning warningCode, String detail) {
        this.warningCode = warningCode;
        this.details = Collections.singletonList(detail);
    }

    @JacocoGenerated
    @JsonProperty("warningCode")
    public Warning getWarningCode() {
        return warningCode;
    }

    @JacocoGenerated
    @JsonProperty("details")
    public List<String> getDetails() {
        return details;
    }

    @JacocoGenerated
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

    @JacocoGenerated
    @Override
    public String toString() {
        return warningCode + " = " + details;
    }

    public enum Warning {
        VERSION_WARNING,
        SUBJECT_WARNING,
        LANGUAGE_MAPPED_TO_UNDEFINED,
        MULTIPLE_DESCRIPTION_PRESENT,
        VOLUME_NOT_NUMBER_WARNING,
        ISSUE_NOT_NUMBER_WARNING,
        PAGE_NUMBER_NOT_NUMBER_WARNING,
        MULTIPLE_UNMAPPABLE_TYPES,
        INVALID_CC_LICENSE,
        PAGE_NUMBER_FORMAT_NOT_RECOGNIZED,
        MULTIPLE_ISBN_VALUES_WARNING,
        EMPTY_COLLECTION
    }

}
