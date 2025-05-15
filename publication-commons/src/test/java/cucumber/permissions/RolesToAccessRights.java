package cucumber.permissions;

import static cucumber.permissions.PermissionsRole.ANY_CURATOR_TYPE;
import static cucumber.permissions.PermissionsRole.EDITOR;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_BY_CONTRIBUTOR_FOR_OTHERS;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_BY_PUBLICATION_OWNER;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_DEGREE;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_DEGREE_EMBARGO;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_FOR_GIVEN_FILE;
import java.util.Map;
import java.util.Set;
import nva.commons.apigateway.AccessRight;

public final class RolesToAccessRights {

    public static final Map<PermissionsRole, Set<AccessRight>> roleToAccessRightsMap = Map.of(
        FILE_CURATOR_BY_CONTRIBUTOR_FOR_OTHERS,
        Set.of(AccessRight.MANAGE_RESOURCES_STANDARD, AccessRight.MANAGE_RESOURCE_FILES),
        FILE_CURATOR_BY_PUBLICATION_OWNER,
        Set.of(AccessRight.MANAGE_RESOURCES_STANDARD, AccessRight.MANAGE_RESOURCE_FILES), FILE_CURATOR_DEGREE_EMBARGO,
        Set.of(AccessRight.MANAGE_DEGREE, AccessRight.MANAGE_DEGREE_EMBARGO, AccessRight.MANAGE_RESOURCES_STANDARD),
        FILE_CURATOR_DEGREE, Set.of(AccessRight.MANAGE_DEGREE, AccessRight.MANAGE_RESOURCES_STANDARD),
        FILE_CURATOR_FOR_GIVEN_FILE, Set.of(AccessRight.MANAGE_RESOURCES_STANDARD, AccessRight.MANAGE_RESOURCE_FILES),
        ANY_CURATOR_TYPE, Set.of(AccessRight.MANAGE_RESOURCES_STANDARD, AccessRight.MANAGE_DOI, AccessRight.SUPPORT,
                                 AccessRight.MANAGE_NVI, AccessRight.MANAGE_PUBLISHING_REQUESTS), EDITOR,
        Set.of(AccessRight.MANAGE_RESOURCES_ALL, AccessRight.MANAGE_OWN_AFFILIATION,
               AccessRight.MANAGE_CHANNEL_CLAIMS));

    private RolesToAccessRights() {
    }
}
