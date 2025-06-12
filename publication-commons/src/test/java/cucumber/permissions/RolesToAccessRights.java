package cucumber.permissions;

import static cucumber.permissions.PermissionsRole.CONTRIBUTOR;
import static cucumber.permissions.PermissionsRole.CREATOR;
import static cucumber.permissions.PermissionsRole.DOI_CURATOR;
import static cucumber.permissions.PermissionsRole.EDITOR;
import static cucumber.permissions.PermissionsRole.EMBARGO_THESIS_CURATOR;
import static cucumber.permissions.PermissionsRole.INTERNAL_IMPORTER;
import static cucumber.permissions.PermissionsRole.NVI_CURATOR;
import static cucumber.permissions.PermissionsRole.PUBLISHING_CURATOR;
import static cucumber.permissions.PermissionsRole.SUPPORT_CURATOR;
import static cucumber.permissions.PermissionsRole.THESIS_CURATOR;
import static nva.commons.apigateway.AccessRight.MANAGE_CHANNEL_CLAIMS;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_IMPORT;
import static nva.commons.apigateway.AccessRight.MANAGE_NVI_CANDIDATES;
import static nva.commons.apigateway.AccessRight.MANAGE_OWN_AFFILIATION;
import static nva.commons.apigateway.AccessRight.MANAGE_OWN_RESOURCES;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCE_FILES;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import java.util.Map;
import java.util.Set;
import nva.commons.apigateway.AccessRight;

public final class RolesToAccessRights {

    public static final Map<PermissionsRole, Set<AccessRight>> roleToAccessRightsMap = Map.ofEntries(
        Map.entry(CREATOR, Set.of(MANAGE_OWN_RESOURCES)),
        Map.entry(CONTRIBUTOR, Set.of(MANAGE_OWN_RESOURCES)),
        Map.entry(PUBLISHING_CURATOR, Set.of(MANAGE_RESOURCES_STANDARD,
                                             MANAGE_PUBLISHING_REQUESTS,
                                             MANAGE_RESOURCE_FILES)),
        Map.entry(NVI_CURATOR, Set.of(MANAGE_RESOURCES_STANDARD,
                                      MANAGE_NVI_CANDIDATES)),
        Map.entry(DOI_CURATOR, Set.of(MANAGE_RESOURCES_STANDARD,
                                      MANAGE_DOI)),
        Map.entry(SUPPORT_CURATOR, Set.of(MANAGE_RESOURCES_STANDARD,
                                          SUPPORT)),
        Map.entry(INTERNAL_IMPORTER, Set.of(MANAGE_IMPORT)),
        Map.entry(THESIS_CURATOR, Set.of(MANAGE_RESOURCES_STANDARD,
                                         MANAGE_DEGREE)),
        Map.entry(EMBARGO_THESIS_CURATOR, Set.of(MANAGE_RESOURCES_STANDARD,
                                                 MANAGE_DEGREE_EMBARGO)),
        Map.entry(EDITOR, Set.of(MANAGE_OWN_AFFILIATION,
                                 MANAGE_RESOURCES_ALL,
                                 MANAGE_CHANNEL_CLAIMS))
    );

    private RolesToAccessRights() {
    }
}
