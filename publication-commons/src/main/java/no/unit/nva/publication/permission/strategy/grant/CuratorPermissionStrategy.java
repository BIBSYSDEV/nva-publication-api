package no.unit.nva.publication.permission.strategy.grant;

import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCE_FILES;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CuratorPermissionStrategy extends GrantPermissionStrategy {

    public static final Logger logger = LoggerFactory.getLogger(CuratorPermissionStrategy.class);

    public CuratorPermissionStrategy(Publication publication, UserInstance userInstance, ResourceService resourceService) {
        super(publication, userInstance, resourceService);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution() ) {
            return false;
        }

        return switch (permission) {
            case UPDATE_FILES -> hasAccessRight(MANAGE_RESOURCE_FILES);
            case UPDATE -> canManageStandardResources();
            case TICKET_PUBLISH -> canManagePublishingRequests() && hasUnpublishedFile();
            case UNPUBLISH -> canManagePublishingRequests() && isPublished();
            case DOI_REQUEST_CREATE -> hasAccessRight(MANAGE_DOI) && userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution();
            case PUBLISHING_REQUEST_CREATE -> canManagePublishingRequests()
                                              && userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution();
            case SUPPORT_REQUEST_CREATE -> hasAccessRight(SUPPORT) && userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution();
            case DOI_REQUEST_APPROVE -> hasAccessRight(MANAGE_DOI);
            case PUBLISHING_REQUEST_APPROVE ->  canManagePublishingRequests();
            case SUPPORT_REQUEST_APPROVE -> hasAccessRight(SUPPORT);
            default -> false;
        };
    }

    private boolean canManageStandardResources() {
        return hasAccessRight(MANAGE_RESOURCES_STANDARD);
    }

    private boolean canManagePublishingRequests() {
        return hasAccessRight(MANAGE_PUBLISHING_REQUESTS);
    }
}
