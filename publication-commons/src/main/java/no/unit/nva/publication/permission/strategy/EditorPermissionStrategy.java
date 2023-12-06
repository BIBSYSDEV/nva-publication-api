package no.unit.nva.publication.permission.strategy;

import static nva.commons.apigateway.AccessRight.EDIT_ALL_NON_DEGREE_RESOURCES;
import static nva.commons.apigateway.AccessRight.PUBLISH_DEGREE;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.RequestInfo;

public class EditorPermissionStrategy extends PermissionStrategy {

    private final RequestInfo requestInfo;
    public EditorPermissionStrategy(RequestInfo requestInfo) {
        super();
        this.requestInfo = requestInfo;
    }

    @Override
    public boolean hasPermission(RequestInfo requestInfo, Publication publication) {
        if (isDegree(publication)) {
            return hasAccessRight(requestInfo, PUBLISH_DEGREE)
                   && hasAccessRight(requestInfo, EDIT_ALL_NON_DEGREE_RESOURCES);
        }

        return hasAccessRight(requestInfo, EDIT_ALL_NON_DEGREE_RESOURCES);
    }

    public static EditorPermissionStrategy fromRequestInfo(RequestInfo requestInfo) {
        return new EditorPermissionStrategy(requestInfo);
    }

    public boolean hasPermission(Publication publication) {
        return hasPermissionToOperateOnDegree(publication)
               || hasAccessRight(requestInfo, EDIT_ALL_NON_DEGREE_RESOURCES);
    }

    private boolean hasPermissionToOperateOnDegree(Publication publication) {
        return isDegree(publication)
               && hasAccessRight(requestInfo, PUBLISH_DEGREE)
               && hasAccessRight(requestInfo, EDIT_ALL_NON_DEGREE_RESOURCES);
    }
}
