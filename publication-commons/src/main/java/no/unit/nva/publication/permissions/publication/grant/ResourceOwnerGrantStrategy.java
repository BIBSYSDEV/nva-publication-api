package no.unit.nva.publication.permissions.publication.grant;

import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationGrantStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public final class ResourceOwnerGrantStrategy extends PublicationStrategyBase implements PublicationGrantStrategy {

    public ResourceOwnerGrantStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!isOwner()) {
            return false;
        }

        return switch (permission) {
            case UPDATE, DOI_REQUEST_CREATE, PUBLISHING_REQUEST_CREATE, SUPPORT_REQUEST_CREATE -> true;
            case UNPUBLISH -> isPublished() && !hasApprovedFiles();
            case DELETE -> isDraft();
            default -> false;
        };
    }
}
