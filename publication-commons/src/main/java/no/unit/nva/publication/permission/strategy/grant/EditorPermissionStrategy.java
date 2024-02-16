package no.unit.nva.publication.permission.strategy.grant;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;

public class EditorPermissionStrategy extends GrantPermissionStrategy {

    public EditorPermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!hasAccessRight(MANAGE_RESOURCES_ALL)) {
            return false;
        }
        return switch (permission) {
            case REPUBLISH -> (isDraft() || isUnpublished());
            case UPDATE -> true;
            case UNPUBLISH -> isPublished();
            case TERMINATE -> isUnpublished();
            case DELETE -> false;
        };
    }

}
