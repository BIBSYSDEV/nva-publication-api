package no.unit.nva.model.time.duration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public final class UndefinedDuration implements Duration {

    private final String value;

    @JsonCreator
    private UndefinedDuration(@JsonProperty("value") String value) {
        this.value = value;
    }

    public static UndefinedDuration fromValue(String value) {
        return new UndefinedDuration(value);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UndefinedDuration that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    public String getValue() {
        return value;
    }
}
