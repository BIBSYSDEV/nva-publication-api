package no.unit.nva.expansion.model;

import lombok.Getter;

@Getter
public enum CollaborationType {

    COLLABORATIVE("collaborative"), NON_COLLABORATIVE("nonCollaborative");

    private final String value;

    CollaborationType(String value) {
        this.value = value;
    }
}
