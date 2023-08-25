package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CollaborationType {

    COLLABORATIVE("Collaborative"), NON_COLLABORATIVE("NonCollaborative");
    private final String value;

    CollaborationType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
