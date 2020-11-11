package no.unit.nva.publication.doi.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PublicationStatus {

    NEW("New"),
    DRAFT("Draft"),
    REJECTED("Rejected"),
    PUBLISHED("Published");

    private final String value;

    PublicationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
