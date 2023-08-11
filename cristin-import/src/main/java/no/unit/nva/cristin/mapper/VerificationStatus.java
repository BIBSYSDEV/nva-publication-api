package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.JacocoGenerated;

public enum VerificationStatus {

    VERIFIED("J"), NOT_VERIFIED("N");
    private final String value;

    VerificationStatus(String value) {
        this.value = value;
    }

    @JacocoGenerated
    @JsonValue
    public String getValue() {
        return value;
    }
}
