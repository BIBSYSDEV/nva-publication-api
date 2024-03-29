package no.unit.nva.publication.permission.strategy.grant;

import static no.unit.nva.publication.model.utils.CuratingInstitutionsUtil.getCuratingInstitutions;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.UserInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CuratorPermissionStrategy extends GrantPermissionStrategy {

    public static final Logger logger = LoggerFactory.getLogger(CuratorPermissionStrategy.class);

    public CuratorPermissionStrategy(Publication publication, UserInstance userInstance, UriRetriever uriRetriever) {
        super(publication, userInstance, uriRetriever);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!userRelatesToPublication()) {
            return false;
        }

        return switch (permission) {
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

    private boolean userIsFromSameInstitutionAsPublication() {
        if (userInstance.getTopLevelOrgCristinId() == null || publication.getResourceOwner() == null) {
            return false;
        }

        return userInstance.getTopLevelOrgCristinId().equals(publication.getResourceOwner().getOwnerAffiliation());
    }

    private boolean userSharesTopLevelOrgWithAtLeastOneContributor() {
        var contributorTopLevelOrgs = getCuratingInstitutions(publication, uriRetriever);
        var userTopLevelOrg = userInstance.getTopLevelOrgCristinId();

        logger.info("found topLevels {} for user {} of {}.",
                    contributorTopLevelOrgs,
                    userInstance.getUser(),
                    userTopLevelOrg);

        return contributorTopLevelOrgs.stream().anyMatch(org -> org.equals(userTopLevelOrg));
    }



    private boolean hasUnpublishedFile() {
        return publication.getAssociatedArtifacts().stream().anyMatch(UnpublishedFile.class::isInstance);
    }

    private boolean canManagePublishingRequests() {
        return hasAccessRight(MANAGE_PUBLISHING_REQUESTS);
    }
}
