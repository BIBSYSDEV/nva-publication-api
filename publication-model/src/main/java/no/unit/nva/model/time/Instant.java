package no.unit.nva.model.time;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Instant implements Time {
    public static final String VALUE_FIELD = "value";

    @JsonProperty(VALUE_FIELD)
    private final java.time.Instant value;

    @Deprecated
    @JsonCreator
    public static Instant fromString(@JsonProperty(VALUE_FIELD) String value) {
        return new Instant(Time.convertToInstant(value));
    }

    public Instant(@JsonProperty(VALUE_FIELD) java.time.Instant value) {
        this.value = value;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Instant)) {
            return false;
        }
        Instant instant = (Instant) o;
        return Objects.equals(value, instant.value);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
