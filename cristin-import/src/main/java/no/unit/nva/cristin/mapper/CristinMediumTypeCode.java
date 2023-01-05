package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.JacocoGenerated;

public enum CristinMediumTypeCode {
    JOURNAL("TIDSSKRIFT"),
    TV("TV"),
    PROFESSIONAL_JOURNAL("FAGBLAD"),
    INTERNET("INTERNETT"),
    NEWSPAPER("AVIS"),
    RADIO("RADIO");

    private final String value;

    CristinMediumTypeCode(String value) {
        this.value = value;
    }

    public static CristinMediumTypeCode fromValue(String value) {
        for (CristinMediumTypeCode type : CristinMediumTypeCode.values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException();
    }

    @JacocoGenerated
    @JsonValue
    public String getValue() {
        return value;
    }
}
