package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Username {

    private final String value;

    @JsonCreator
    public Username(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Username && value.equals(((Username) obj).getValue());
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
