package no.unit.nva.publication.permission.strategy;

import no.unit.nva.model.Publication;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;

public abstract class PermissionStrategy {

    public abstract boolean hasPermission(RequestInfo requestInfo, Publication publication);

    public static boolean hasAccessRight(RequestInfo requestInfo, AccessRight accessRight) {
        return requestInfo.userIsAuthorized(accessRight.name());
    }
}