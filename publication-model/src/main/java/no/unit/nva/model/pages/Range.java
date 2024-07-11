package no.unit.nva.model.pages;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Range implements Pages {
    public static final String NON_SPACE_WHITESPACE = "[\\n\\r\\t]+";
    public static final String EMPTY_STRING = "";

    private final String begin;
    private final String end;

    /**
     * Default constructor for Range ensures that a range has a specified beginning and an end, and contains
     * no unnecessary whitespace, nor members that are only whitespace.
     *
     * @param begin The beginning of the range.
     * @param end   The end of the range
     */
    @JsonCreator
    public Range(@JsonProperty("begin") String begin, @JsonProperty("end") String end) {
        String sanitizedBegin = sanitize(begin);
        String sanitizedEnd = sanitize(end);
        this.begin = verifyOrCreateSinglePageRange(sanitizedBegin, sanitizedEnd);
        this.end = verifyOrCreateSinglePageRange(sanitizedEnd, sanitizedBegin);
    }

    private String verifyOrCreateSinglePageRange(String input, String comparison) {
        return nonNull(input) ? input : comparison;
    }

    private String sanitize(String input) {
        return nonNull(input) ? removeWhiteSpace(input) : null;
    }

    private String removeWhiteSpace(String input) {
        String trimmed = input.replaceAll(NON_SPACE_WHITESPACE, EMPTY_STRING).trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Range(Builder builder) {
        this(builder.begin, builder.end);
    }

    public String getBegin() {
        return begin;
    }

    public String getEnd() {
        return end;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Range)) {
            return false;
        }
        Range range = (Range) o;
        return Objects.equals(begin, range.begin)
                && Objects.equals(end, range.end);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(begin, end);
    }

    public static final class Builder {
        private String begin;
        private String end;

        public Builder() {
        }

        public Builder withBegin(String begin) {
            this.begin = begin;
            return this;
        }

        public Builder withEnd(String end) {
            this.end = end;
            return this;
        }

        public Range build() {
            return new Range(this);
        }
    }
}
