package cucumber.permissions;

import static java.util.Arrays.stream;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Set;
import java.util.stream.Collectors;

public enum PermissionsRole {
    UNAUTHENTICATED("unauthenticated"),
    AUTHENTICATED_BUT_NO_ACCESS("authenticated"),
    PUBLISHING_CURATOR("publishing curator"),
    NVI_CURATOR("nvi curator"),
    DOI_CURATOR("doi curator"),
    SUPPORT_CURATOR("support curator"),
    CREATOR("creator", "publication creator"),
    CONTRIBUTOR("contributor"),
    INTERNAL_IMPORTER("internal importer"),
    THESIS_CURATOR("thesis curator"),
    EMBARGO_THESIS_CURATOR("embargo thesis curator"),
    EDITOR("editor"),
    RELATED_EXTERNAL_CLIENT("related external client"),
    NOT_RELATED_EXTERNAL_CLIENT("not related external client");

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
        var result = stream(values())
            .filter(permissionsRole -> stream(permissionsRole.values).anyMatch(search::equalsIgnoreCase))
            .collect(Collectors.toSet());
        if (result.isEmpty()) {
            throw new IllegalArgumentException("No PermissionsRole found for: " + search);
        }
        return result;
    }
}