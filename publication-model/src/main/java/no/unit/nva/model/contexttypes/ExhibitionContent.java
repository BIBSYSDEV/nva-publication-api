package no.unit.nva.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ExhibitionContent implements BasicContext {

    public static final int STATIC_VALUE_FOR_HASH_CODE = 103_645;

    @Override
    public int hashCode() {
        return STATIC_VALUE_FOR_HASH_CODE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof BasicContext;
    }
}
