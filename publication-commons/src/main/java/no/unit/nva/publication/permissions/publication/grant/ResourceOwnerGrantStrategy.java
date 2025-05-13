package no.unit.nva.publication.permissions.publication.grant;

import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationGrantStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public final class ResourceOwnerGrantStrategy extends PublicationStrategyBase implements PublicationGrantStrategy {

    public ResourceOwnerGrantStrategy(Resource resource, UserInstance userInstance) {
        super(resource, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!isOwner()) {
            return false;
        }

        return switch (permission) {
            case UPDATE,
                 PARTIAL_UPDATE,
                 DOI_REQUEST_CREATE,
                 PUBLISHING_REQUEST_CREATE,
                 SUPPORT_REQUEST_CREATE,
                 UPLOAD_FILE -> true;
            case UNPUBLISH -> isPublished() && !hasApprovedFiles();
            case DELETE -> isDraft();
            case UPDATE_FILES, READ_HIDDEN_FILES, REPUBLISH, TERMINATE, DOI_REQUEST_APPROVE,
                 PUBLISHING_REQUEST_APPROVE, APPROVE_FILES, SUPPORT_REQUEST_APPROVE -> false;
        };
    }
}
