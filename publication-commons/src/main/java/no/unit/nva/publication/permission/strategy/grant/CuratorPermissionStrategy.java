package no.unit.nva.publication.permission.strategy.grant;

import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCE_FILES;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CuratorPermissionStrategy extends GrantPermissionStrategy {

    public static final Logger logger = LoggerFactory.getLogger(CuratorPermissionStrategy.class);

    public CuratorPermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!userRelatesToPublication()) {
            return false;
        }

        return switch (permission) {
            case UPDATE_FILES -> hasAccessRight(MANAGE_RESOURCE_FILES);
            case UPDATE -> canManageStandardResources();
            case TICKET_PUBLISH -> canManagePublishingRequests() && hasUnpublishedFile();
            case UNPUBLISH -> canManageStandardResources() && isPublished();
            default -> false;
        };
    }

    private boolean canManageStandardResources() {
        return hasAccessRight(MANAGE_RESOURCES_STANDARD);
    }

    private boolean userRelatesToPublication() {
        return userIsFromSameInstitutionAsPublication() || userSharesTopLevelOrgWithAtLeastOneContributor();
    }

    private boolean canManagePublishingRequests() {
        return hasAccessRight(MANAGE_PUBLISHING_REQUESTS);
    }
}
