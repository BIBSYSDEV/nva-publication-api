package no.unit.nva.expansion.model;

import lombok.Getter;

@Getter
public enum CollaborationType {

    MULTIPLE_ORGANIZATIONS("multipleOrganizations"), SINGLE_ORGANIZATION("singleOrganization");

    private final String value;

    CollaborationType(String value) {
        this.value = value;
    }
}
