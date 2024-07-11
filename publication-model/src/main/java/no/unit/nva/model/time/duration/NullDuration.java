package no.unit.nva.model.time.duration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public final class NullDuration implements Duration {

    private static final int STATIC_VALUE_FOR_HASH_CODE = 55_545;

    @JsonCreator
    private NullDuration() {
    }

    public static NullDuration create() {
        return new NullDuration();
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
        return o instanceof NullDuration;
    }
}
