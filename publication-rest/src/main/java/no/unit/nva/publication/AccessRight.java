package no.unit.nva.publication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public enum AccessRight {

    APPROVE_DOI_REQUEST,
    REJECT_DOI_REQUEST,
    READ_DOI_REQUEST,
    EDIT_OWN_INSTITUTION_RESOURCES;

    private static final Map<String, AccessRight> index = createIndex();

    /**
     * Creates an AccessRight instance from a string (case insensitive).
     *
     * @param accessRight string representation of access right
     * @return an AccessRight instance.
     */
    @JsonCreator
    public static AccessRight fromString(String accessRight) {

        String formattedString = formatString(accessRight);
        if (index.containsKey(formattedString)) {
            return index.get(formattedString);
        } else {
            throw new RuntimeException("Unknown Access Right:"+accessRight);
        }
    }

    @Override
    @JsonValue
    public String toString() {
        return formatString(this.name());
    }

    private static String formatString(String accessRightString) {
        return accessRightString.toUpperCase(Locale.getDefault());
    }

    private static Map<String, AccessRight> createIndex() {
        return Arrays.stream(AccessRight.values())
            .collect(Collectors.toMap(AccessRight::toString, v -> v));
    }
}
