package no.unit.nva.expansion.model;

import lombok.Getter;

@Getter
public enum CollaborationType {

    COLLABORATIVE("Collaborative"), NON_COLLABORATIVE("NonCollaborative");

    private final String value;

    CollaborationType(String value) {
        this.value = value;
    }
}
