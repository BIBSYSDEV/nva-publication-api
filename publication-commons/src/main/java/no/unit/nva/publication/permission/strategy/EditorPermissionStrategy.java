package no.unit.nva.publication.permission.strategy;

import static nva.commons.apigateway.AccessRight.EDIT_ALL_NON_DEGREE_RESOURCES;
import static nva.commons.apigateway.AccessRight.PUBLISH_DEGREE;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.RequestInfo;

public class EditorPermissionStrategy extends PermissionStrategy {

    @Override
    public boolean hasPermission(RequestInfo requestInfo, Publication publication) {
        if (isDegree(publication)) {
            return hasAccessRight(requestInfo, PUBLISH_DEGREE);
        }

        return hasAccessRight(requestInfo, EDIT_ALL_NON_DEGREE_RESOURCES);
    }
}
