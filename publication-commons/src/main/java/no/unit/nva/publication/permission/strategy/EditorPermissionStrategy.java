package no.unit.nva.publication.permission.strategy;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.RequestInfo;

public class EditorPermissionStrategy extends PermissionStrategy {

    private final RequestInfo requestInfo;

    public EditorPermissionStrategy(RequestInfo requestInfo) {
        super();
        this.requestInfo = requestInfo;
    }

    public static EditorPermissionStrategy fromRequestInfo(RequestInfo requestInfo) {
        return new EditorPermissionStrategy(requestInfo);
    }

    @Override
    public boolean hasPermission(RequestInfo requestInfo, Publication publication) {
        if (isDegree(publication)) {
            return hasAccessRight(requestInfo, MANAGE_DEGREE)
                   && hasAccessRight(requestInfo, MANAGE_RESOURCES_ALL);
        }

        return hasAccessRight(requestInfo, MANAGE_RESOURCES_ALL);
    }

    public boolean hasPermission(Publication publication) {
        return hasPermissionToOperateOnDegree(publication)
               || hasAccessRight(requestInfo, MANAGE_RESOURCES_ALL);
    }

    private boolean hasPermissionToOperateOnDegree(Publication publication) {
        return isDegree(publication) && hasAccessRight(requestInfo, MANAGE_DEGREE)
               && hasAccessRight(requestInfo, MANAGE_RESOURCES_ALL);
    }
}
