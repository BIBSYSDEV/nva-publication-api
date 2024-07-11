package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import nva.commons.core.JacocoGenerated;

/**
 * This class represents a state for publications that do not and cannot have
 * associated artifacts.
 */
@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(NullAssociatedArtifact.TYPE_NAME)
public class NullAssociatedArtifact implements AssociatedArtifact {

    private static final int STATIC_VALUE_FOR_HASH_CODE = 88_961;
    public static final String TYPE_NAME = "NullAssociatedArtifact";

    public NullAssociatedArtifact() {
        // NO-OP
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return STATIC_VALUE_FOR_HASH_CODE;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof NullAssociatedArtifact;
    }
}
