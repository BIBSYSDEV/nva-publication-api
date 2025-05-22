package cucumber.permissions;

import static java.util.Arrays.stream;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Set;
import java.util.stream.Collectors;

public enum PermissionsRole {
    FILE_CURATOR_BY_CONTRIBUTOR_FOR_OTHERS("file curators for other contributors"),
    FILE_CURATOR_BY_PUBLICATION_OWNER("file curator by publication owner at x"),
    FILE_CURATOR_FOR_GIVEN_FILE("file curator at x"),
    FILE_CURATOR_DEGREE_EMBARGO("degree embargo file curator"),
    FILE_CURATOR_DEGREE("degree file curator"),
    NON_DEGREE_CURATOR_TYPE("file, support, doi or nvi curator"),
    UNAUTHENTICATED("unauthenticated"),
    AUTHENTICATED_BUT_NO_ACCESS("everyone"),
    CONTRIBUTOR_FOR_GIVEN_FILE("contributor at x"),
    OTHER_CONTRIBUTORS("other contributors", "contributor"),
    NOT_RELATED_EXTERNAL_CLIENT("not related external client",  "external client"),
    RELATED_EXTERNAL_CLIENT("related external client"),
    PUBLICATION_OWNER("publication owner"),
    EDITOR("editor"),;

    private final String[] values;

    PermissionsRole(String... values) {
        this.values = values;
    }

    @JsonValue
    public String getValue() {
        return values[0];
    }

    /**
     * Lookup enum by value.
     *
     * @param search string to look for
     * @return set of enums
     */
    public static Set<PermissionsRole> lookup(String search) {
        return stream(values())
                   .filter(permissionsRole ->
                               stream(permissionsRole.values)
                                   .anyMatch(value -> search.toLowerCase().startsWith(value.toLowerCase()))
                   )
                   .collect(Collectors.toSet());
    }
}