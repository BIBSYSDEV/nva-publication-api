package no.unit.nva.publication.permission.strategy.grant;

import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.UserInstance;

public class ResourceOwnerPermissionStrategy extends GrantPermissionStrategy {

    public ResourceOwnerPermissionStrategy(Publication publication, UserInstance userInstance, UriRetriever uriRetriever) {
        super(publication, userInstance, uriRetriever);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (userInstance.isExternalClient() || !isOwner()) {
            return false;
        }

        return switch (permission) {
            case UPDATE -> true;
            case UNPUBLISH -> isPublished();
            case DELETE -> isDraft();
            default -> false;
        };
    }
}
