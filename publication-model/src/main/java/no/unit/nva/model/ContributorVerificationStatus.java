package no.unit.nva.model;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContributorVerificationStatus {

    VERIFIED("Verified"),

    /**
     * For use-cases where querying for verification status returns status forbidden.
     */
    CANNOT_BE_ESTABLISHED("CannotBeEstablished"),
    NOT_VERIFIED("NotVerified");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid ContributorVerificationStatus, expected one "
                                                        + "of: %s";
    public static final String DELIMITER = ", ";
    private final String value;

    ContributorVerificationStatus(String value) {
        this.value = value;
    }

    /**
     * Lookup enum by value.
     *
     * @param candidate value
     * @return enum
     */
    @JsonCreator
    public static ContributorVerificationStatus parse(String candidate) {
        return stream(values())
                   .filter(nameType -> nameType.getValue().equalsIgnoreCase(candidate))
                   .findAny()
                   .orElseThrow(() -> new IllegalArgumentException(
                       format(ERROR_MESSAGE_TEMPLATE, candidate, stream(ContributorVerificationStatus.values())
                                                                     .map(ContributorVerificationStatus::toString)
                                                                     .collect(joining(DELIMITER)))));
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
