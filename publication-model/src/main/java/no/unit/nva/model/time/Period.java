package no.unit.nva.model.time;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;
import nva.commons.core.StringUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Period implements Time {

    public static final String FROM_FIELD = "from";
    public static final String TO_FIELD = "to";
    @JsonProperty(FROM_FIELD)
    private final java.time.Instant from;
    @JsonProperty(TO_FIELD)
    private final java.time.Instant to;

    // The conversion methods should be removed following migration

    public Period(java.time.Instant from, java.time.Instant to) {
        this.from = from;
        this.to = to;
    }

    public java.time.Instant getFrom() {
        return from;
    }

    public java.time.Instant getTo() {
        return to;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getFrom(), getTo());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Period that)) {
            return false;
        }
        return Objects.equals(getFrom(), that.getFrom())
               && Objects.equals(getTo(), that.getTo());
    }

    @Deprecated
    @JsonCreator
    private static Period fromLegacy(@JsonProperty(FROM_FIELD) String from,
                                     @JsonProperty(TO_FIELD) String to) {
        return new Period(Time.convertToInstant(from), extractToDate(to));
    }

    private static java.time.Instant extractToDate(String to) {
        return shouldBeNullToTime(to) ? null : Time.convertToInstant(to);
    }

    private static boolean shouldBeNullToTime(String to) {
        return StringUtils.isBlank(to);
    }
}
