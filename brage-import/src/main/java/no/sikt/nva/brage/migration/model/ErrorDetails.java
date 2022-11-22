package no.sikt.nva.brage.migration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class ErrorDetails {
    private final Error errorCode;
    private final List<String> details;

    public ErrorDetails(Error errorCode, List<String> details) {
        this.errorCode = errorCode;
        this.details = details;
    }

    @JsonProperty("errorCode")
    public Error getErrorCode() {
        return errorCode;
    }

    @JsonProperty("details")
    public List<String> getDetails() {
        return details;
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorCode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ErrorDetails)) {
            return false;
        }
        var errorDetail = (ErrorDetails) o;
        return this.errorCode.equals(errorDetail.errorCode);
    }

    @Override
    public String toString() {
        return errorCode + " = " + details;
    }

    public enum Error {
        CRISTIN_ID_PRESENT,
        INVALID_TYPE,
        MANY_UNMAPPABLE_TYPES,
        INVALID_ISSN,
        INVALID_DATE_ERROR,
        DATE_NOT_PRESENT_ERROR,
        JOURNAL_NOT_IN_CHANNEL_REGISTER,
        PUBLISHER_NOT_IN_CHANNEL_REGISTER,
        INVALID_ISBN,
        INVALID_DOI_OFFLINE_CHECK,
        INVALID_DOI_ONLINE_CHECK,
        INVALID_LANGUAGE,
        MULTIPLE_SEARCH_RESULTS_IN_CHANNEL_REGISTER_BY_VALUE
    }

}
