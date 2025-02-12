package cucumber.permissions;

import static java.util.Arrays.stream;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Set;
import java.util.stream.Collectors;

public enum PermissionsRole {
    FILE_CURATOR("file curator"),
    FILE_CURATOR_FOR_GIVEN_FILE("file curator at x"),
    EVERYONE("everyone"),
    CONTRIBUTOR_FOR_GIVEN_FILE("contributor at x"),
    OTHER_CONTRIBUTORS("other contributors", "contributor"),
    FILE_OWNER("uploader"),
    EXTERNAL_CLIENT("external client"),
    PUBLICATION_OWNER("publication owner");

    private final String[] values;

    PermissionsRole(String... s) {
        this.values = s;
    }

    @JsonValue
    public String getValue() {

        return values[0];
    }

    /**
     * Lookup enum by value.
     *
     * @param value value
     * @return set of enums
     */
    public static Set<PermissionsRole> lookup(String value) {
        return stream(values())
                   .filter(permissionsRole ->
                               stream(permissionsRole.values)
                                   .anyMatch(v -> value.toLowerCase().contains(v.toLowerCase()))
                   )
                   .collect(Collectors.toSet());
    }
}