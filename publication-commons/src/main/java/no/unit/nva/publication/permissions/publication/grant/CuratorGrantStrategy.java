package no.unit.nva.publication.permissions.publication.grant;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCE_FILES;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationGrantStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CuratorGrantStrategy extends PublicationStrategyBase implements PublicationGrantStrategy {

    public static final Logger logger = LoggerFactory.getLogger(CuratorGrantStrategy.class);

    public CuratorGrantStrategy(Resource resource, UserInstance userInstance) {
        super(resource, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!userRelatesToPublication()) {
            return false;
        }

        return switch (permission) {
            case UPDATE_FILES -> hasAccessRight(MANAGE_RESOURCE_FILES);
            case UPDATE,
                 PARTIAL_UPDATE,
                 SUPPORT_REQUEST_CREATE,
                 DOI_REQUEST_CREATE,
                 PUBLISHING_REQUEST_CREATE,
                 UPLOAD_FILE -> isCurator();
            case UNPUBLISH -> hasApprovedFiles() ? canApproveFiles() && isPublished() : isCurator();
            case DOI_REQUEST_APPROVE -> hasAccessRight(MANAGE_DOI);
            case PUBLISHING_REQUEST_APPROVE,
                 READ_HIDDEN_FILES, APPROVE_FILES -> canApproveFiles();
            case SUPPORT_REQUEST_APPROVE -> hasAccessRight(SUPPORT);
            default -> false;
        };
    }

    private boolean isCurator() {
        return hasAccessRight(MANAGE_RESOURCES_STANDARD);
    }

    private boolean canApproveFiles() {
        return resource.isDegree()
                   ? hasAccessRight(MANAGE_DEGREE) || hasAccessRight(MANAGE_DEGREE_EMBARGO)
                   : hasAccessRight(MANAGE_PUBLISHING_REQUESTS);
    }
}
