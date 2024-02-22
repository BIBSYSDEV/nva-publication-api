package no.unit.nva.publication.permission.strategy.grant;

import static no.unit.nva.publication.utils.RdfUtils.getTopLevelOrgUri;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
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
        if (!canManageStandardResources() || !userRelatesToPublication()) {
            return false;
        }

        return switch (permission) {
            case UPDATE -> true;
            case TICKET_PUBLISH -> isPublishableState() && canManagePublishingRequests();
            case UNPUBLISH -> isPublished();
            default -> false;
        };
    }

    private boolean isPublishableState() {
        return isUnpublished() || isDraft();
    }

    private boolean canManageStandardResources() {
        return hasAccessRight(MANAGE_RESOURCES_STANDARD);
    }

    private boolean canManagePublishingRequests() {
        return hasAccessRight(MANAGE_PUBLISHING_REQUESTS);
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
        var contributorTopLevelOrgs = getContributorTopLevelOrgs();
        var userTopLevelOrg = userInstance.getTopLevelOrgCristinId();

        logger.info("found topLevels {} for user {} of {}. Verified contributors: {} ",
                    contributorTopLevelOrgs,
                    userInstance.getUser().toString(),
                    userTopLevelOrg,
                    getVerifiedContributors().collect(Collectors.toSet()));

        return contributorTopLevelOrgs.stream().anyMatch(org -> org.equals(userTopLevelOrg));
    }

    private Set<URI> getContributorTopLevelOrgs() {
        return getVerifiedContributors()
                   .flatMap(contributor ->
                                contributor.getAffiliations().stream()
                                    .filter(Organization.class::isInstance)
                                    .map(Organization.class::cast)
                                    .map(Organization::getId))
                   .collect(Collectors.toSet())
                   .parallelStream()
                   .map(orgId -> getTopLevelOrgUri(uriRetriever, orgId))
                   .collect(Collectors.toSet());
    }

    private Stream<Contributor> getVerifiedContributors() {
        return publication.getEntityDescription().getContributors()
                   .stream()
                   .filter(this::isVerifiedContributor);
    }
}
