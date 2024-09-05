package no.unit.nva.publication.permission.strategy.grant;

import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;

public class EditorPermissionStrategy extends GrantPermissionStrategy {

    public EditorPermissionStrategy(Publication publication, UserInstance userInstance, ResourceService resourceService) {
        super(publication, userInstance, resourceService);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!hasAccessRight(MANAGE_RESOURCES_ALL)) {
            return false;
        }
        return switch (permission) {
            case TICKET_PUBLISH -> isDraft() || isUnpublished();
            case UPDATE -> true;
            case UNPUBLISH -> isPublished();
            case TERMINATE -> isUnpublished();
            case REPUBLISH -> userBelongsToCuratingInstitution() && isUnpublished();
            case DOI_REQUEST_CREATE,
                 PUBLISHING_REQUEST_CREATE,
                 SUPPORT_REQUEST_CREATE -> userBelongsToCuratingInstitution();
            case DELETE,
                 UPDATE_FILES,
                 DOI_REQUEST_APPROVE,
                 PUBLISHING_REQUEST_APPROVE,
                 SUPPORT_REQUEST_APPROVE -> false;
        };
    }

}
