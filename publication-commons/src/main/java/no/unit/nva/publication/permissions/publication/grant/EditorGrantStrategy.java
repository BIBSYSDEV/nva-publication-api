package no.unit.nva.publication.permissions.publication.grant;

import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationGrantStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public final class EditorGrantStrategy extends PublicationStrategyBase implements PublicationGrantStrategy {

    public EditorGrantStrategy(Publication publication,
                               UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!hasAccessRight(MANAGE_RESOURCES_ALL)) {
            return false;
        }
        return switch (permission) {
            case UPDATE -> true;
            case UNPUBLISH -> userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution() && isPublished();
            case REPUBLISH, TERMINATE -> userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution() && isUnpublished();
            case DOI_REQUEST_CREATE,
                 PUBLISHING_REQUEST_CREATE,
                 SUPPORT_REQUEST_CREATE,
                 READ_HIDDEN_FILES -> userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution();
            case DELETE,
                 UPDATE_FILES,
                 DOI_REQUEST_APPROVE,
                 PUBLISHING_REQUEST_APPROVE,
                 SUPPORT_REQUEST_APPROVE,
                 UPLOAD_FILE -> false;
        };
    }
}
