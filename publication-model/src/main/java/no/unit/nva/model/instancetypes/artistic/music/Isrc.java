package no.unit.nva.model.instancetypes.artistic.music;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import nva.commons.core.JacocoGenerated;

import static java.util.Objects.nonNull;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class Isrc {

    public static final Supplier<Pattern> VALIDATION_PATTERN =
        () -> Pattern.compile("^[A-Z]{2}[0-9A-Z]{3}\\d{2}\\d{5}$");
    public static final String VALUE_FIELD_NAME = "value";

    @JsonProperty(VALUE_FIELD_NAME)
    private final String value;

    @JsonCreator
    public Isrc(@JsonProperty(VALUE_FIELD_NAME) String isrc) throws InvalidIsrcException {
        this.value = validate(isrc);
    }

    public String getValue() {
        return value;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getValue());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Isrc)) {
            return false;
        }
        Isrc isrc = (Isrc) o;
        return Objects.equals(getValue(), isrc.getValue());
    }

    private String validate(String isrc) throws InvalidIsrcException {
        if (nonNull(isrc) && notValid(isrc)) {
            throw new InvalidIsrcException(isrc);
        }
        return isrc;
    }

    private boolean notValid(String isrc) {
        return !isValid(isrc);
    }

    private boolean isValid(String isrc) {
        var matcher = VALIDATION_PATTERN.get().matcher(isrc);
        return matcher.matches();
    }
}
