package no.sikt.nva.brage.migration.merger.findexistingpublication;

import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.JacocoGenerated;

public enum DuplicateDetectionCause {
    CRISTIN_DUPLICATES("Cristin-duplicates"),
    DOI_DUPLICATES("DOI-duplicates"),
    ISBN_DUPLICATES("ISBN-duplicates"),
    HANDLE_DUPLICATES("Handle-duplicates"),
    TITLE_DUPLICATES("Title-duplicates");

    private final String value;

    @JacocoGenerated
    DuplicateDetectionCause(String value) {
        this.value = value;
    }

    @JacocoGenerated
    public static DuplicateDetectionCause fromValue(String value) {
        for (DuplicateDetectionCause cause : DuplicateDetectionCause.values()) {
            if (cause.getValue().equalsIgnoreCase(value)) {
                return cause;
            }
        }
        return null;
    }

    @JacocoGenerated
    @JsonValue
    public String getValue() {
        return value;
    }
}
