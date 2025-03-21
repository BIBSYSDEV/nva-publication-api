package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RelationType {

    METADATA_SOURCE("metadataSource"), SAME_AS("sameAs"), DATASET("dataset"), MENTION("mention");

    private final String value;

    RelationType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
