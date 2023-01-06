package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;

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
        return
            Arrays.stream(CristinMediumTypeCode.values())
                .filter(type -> type.getValue().equalsIgnoreCase(value))
                .collect(SingletonCollector.collect());
    }

    @JacocoGenerated
    @JsonValue
    public String getValue() {
        return value;
    }
}
