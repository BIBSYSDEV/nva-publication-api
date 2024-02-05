package no.unit.nva.publication.permission.strategy;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
import java.net.URI;
import java.util.List;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;

public class EditorPermissionStrategy extends PermissionStrategy {

    public EditorPermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }


    @Override
    public boolean hasPermissionToDelete() {
        return canManage();
    }

    @Override
    public boolean hasPermissionToUnpublish() {
        return canManage();
    }

    @Override
    public boolean hasPermissionToUpdate() {
        return canManage();
    }

    private boolean canManage() {
        if (isDegree()) {
            return hasAccessRight(MANAGE_DEGREE)
                   && hasAccessRight(MANAGE_RESOURCES_ALL);
        }

        return hasAccessRight(MANAGE_RESOURCES_ALL);
    }
}
