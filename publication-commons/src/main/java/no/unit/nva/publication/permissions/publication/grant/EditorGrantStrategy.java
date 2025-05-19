package no.unit.nva.publication.permissions.publication.grant;

import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationGrantStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public final class EditorGrantStrategy extends PublicationStrategyBase implements PublicationGrantStrategy {

    public EditorGrantStrategy(Resource resource, UserInstance userInstance) {
        super(resource, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!hasAccessRight(MANAGE_RESOURCES_ALL)) {
            return false;
        }
        return switch (permission) {
            case UPDATE, PARTIAL_UPDATE -> true;
            case UNPUBLISH -> userRelatesToPublication() && isPublished();
            case REPUBLISH, TERMINATE ->
                userRelatesToPublication() && isUnpublished();
            case DOI_REQUEST_CREATE,
                 PUBLISHING_REQUEST_CREATE,
                 SUPPORT_REQUEST_CREATE,
                 READ_HIDDEN_FILES -> userRelatesToPublication();
            default -> false;
        };
    }
}
