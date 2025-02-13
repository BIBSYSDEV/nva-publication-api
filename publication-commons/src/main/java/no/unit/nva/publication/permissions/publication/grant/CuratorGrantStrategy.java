package no.unit.nva.publication.permissions.publication.grant;

import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCE_FILES;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationGrantStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CuratorGrantStrategy extends PublicationStrategyBase implements PublicationGrantStrategy {

    public static final Logger logger = LoggerFactory.getLogger(CuratorGrantStrategy.class);

    public CuratorGrantStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution()) {
            return false;
        }

        return switch (permission) {
            case UPDATE_FILES, UPLOAD_FILE -> hasAccessRight(MANAGE_RESOURCE_FILES);
            case UPDATE, SUPPORT_REQUEST_CREATE, DOI_REQUEST_CREATE, PUBLISHING_REQUEST_CREATE -> canManageStandardResources();
            case UNPUBLISH -> canManagePublishingRequests() && isPublished();
            case DOI_REQUEST_APPROVE -> hasAccessRight(MANAGE_DOI);
            case PUBLISHING_REQUEST_APPROVE,
                 READ_HIDDEN_FILES -> canManagePublishingRequests();
            case SUPPORT_REQUEST_APPROVE -> hasAccessRight(SUPPORT);
            case REPUBLISH, DELETE, TERMINATE -> false;
        };
    }

    private boolean canManageStandardResources() {
        return hasAccessRight(MANAGE_RESOURCES_STANDARD);
    }

    private boolean canManagePublishingRequests() {
        return hasAccessRight(MANAGE_PUBLISHING_REQUESTS);
    }
}
