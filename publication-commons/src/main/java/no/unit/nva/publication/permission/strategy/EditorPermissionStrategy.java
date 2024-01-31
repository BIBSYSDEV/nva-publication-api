package no.unit.nva.publication.permission.strategy;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.RequestInfo;

public class EditorPermissionStrategy extends PermissionStrategy {

    @Override
    public boolean hasPermissionToDelete(RequestInfo requestInfo, Publication publication) {
        return canManage(requestInfo, publication);
    }

    @Override
    public boolean hasPermissionToUnpublish(RequestInfo requestInfo, Publication publication) {
        return canManage(requestInfo, publication);
    }

    @Override
    public boolean hasPermissionToUpdate(RequestInfo requestInfo, Publication publication) {
        return canManage(requestInfo, publication);
    }

    private static boolean canManage(RequestInfo requestInfo, Publication publication) {
        if (isDegree(publication)) {
            return hasAccessRight(requestInfo, MANAGE_DEGREE)
                   && hasAccessRight(requestInfo, MANAGE_RESOURCES_ALL);
        }

        return hasAccessRight(requestInfo, MANAGE_RESOURCES_ALL);
    }
}
