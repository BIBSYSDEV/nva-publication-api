package no.unit.nva.publication.permission.strategy;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.UserInstance;

public class EditorPermissionStrategy extends PermissionStrategy {

    public EditorPermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    protected boolean hasPermission(PublicationPermission permission) {
        return switch (permission) {
            case UPDATE, DELETE, UNPUBLISH -> canManage();
            default -> false;
        };
    }

    private boolean canManage() {
        if (isDegree()) {
            return hasAccessRight(MANAGE_DEGREE)
                   && hasAccessRight(MANAGE_RESOURCES_ALL);
        }

        return hasAccessRight(MANAGE_RESOURCES_ALL);
    }
}
