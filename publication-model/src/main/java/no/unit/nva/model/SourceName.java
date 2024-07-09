package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/**
 * A record representing a source name.
 *
 * @param system the system name of the source. e.g. "brage", "cristin" or "nva"
 * @param instanceName the instance name of the source. e.g. "194.0.0.0", "20754.0.0.0", etc
 */
public record SourceName(String system, String instanceName) {

    public static final String SEPARATOR = "@";
    public static final String BRAGE_SYSTEM = "brage";
    public static final String CRISTIN_SYSTEM = "cristin";

    @JsonValue
    @Override
    public String toString() {
        return validate(system == null ? instanceName : system + SEPARATOR + instanceName);
    }

    private String validate(String sourceName) {
        var stripped = sourceName.toLowerCase(Locale.ROOT).strip();
        if (!stripped.equals(sourceName)) {
            throw new IllegalArgumentException("SourceName system and instanceName must be lower case and trimmed");
        }
        return sourceName;
    }

    @SuppressWarnings("PMD.NullAssignment")
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    SourceName(String sourceName) {
        this(sourceName.contains(SEPARATOR) ? sourceName.split(SEPARATOR)[0] : null,
             sourceName.contains(SEPARATOR) ? sourceName.split(SEPARATOR)[1] : sourceName);
    }

    /**
     * A record representing a brage source for a given instance.
     *
     * @param instanceName the instance name of the source. e.g. "194.0.0.0", "20754.0.0.0", etc
     */
    public static SourceName fromBrage(String instanceName) {
        return new SourceName(BRAGE_SYSTEM, instanceName);
    }

    /**
     * A record representing a cristin source for a given instance.
     *
     * @param instanceName the instance name of the source. e.g. "194.0.0.0", "20754.0.0.0", etc
     */
    public static SourceName fromCristin(String instanceName) {
        return new SourceName(CRISTIN_SYSTEM, instanceName);
    }
}
