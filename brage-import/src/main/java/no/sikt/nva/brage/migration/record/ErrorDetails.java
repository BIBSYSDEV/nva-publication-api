package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class ErrorDetails {

    private final Error errorCode;
    private final List<String> details;

    public ErrorDetails(@JsonProperty("errorCode") Error errorCode,
                        @JsonProperty("details") List<String> details) {
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
        INVALID_DC_TYPE,
        MULTIPLE_UNMAPPABLE_TYPES,
        INVALID_ISSN,
        INVALID_DC_DATE_ISSUED,
        DATE_NOT_PRESENT_DC_DATE_ISSUED,
        MISSING_DC_ISSN_AND_DC_JOURNAL,
        DC_JOURNAL_NOT_IN_CHANNEL_REGISTER,
        DC_PUBLISHER_NOT_IN_CHANNEL_REGISTER,
        MULTIPLE_DC_VERSION_VALUES,
        MISSING_DC_PUBLISHER,
        INVALID_DC_IDENTIFIER_DOI_OFFLINE_CHECK,
        INVALID_DOI_ONLINE_CHECK,
        INVALID_DC_LANGUAGE,
        MULTIPLE_DC_LANGUAGES_PRESENT,
        DUPLICATE_JOURNAL_IN_CHANNEL_REGISTER,
        DUPLICATE_PUBLISHER_IN_CHANNEL_REGISTER,
        INVALID_DC_RIGHTS_URI,
        MULTIPLE_VALUES,
        CONTENT_FILE_MISSING
    }
}
