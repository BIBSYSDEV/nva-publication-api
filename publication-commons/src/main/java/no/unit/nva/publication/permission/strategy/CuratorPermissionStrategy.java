package no.unit.nva.publication.permission.strategy;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.UserInstance;

public class CuratorPermissionStrategy extends PermissionStrategy {

    public CuratorPermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    protected boolean hasPermission(PublicationPermission permission) {
        return switch (permission) {
            case UPDATE, DELETE, UNPUBLISH -> canManage();
            default -> false;
        };
    }

    private boolean canManage() {
        if (userIsFromSameInstitutionAsPublication() ||
            userAndContributorInTheSameInstitution()) {
            if (isDegree()) {
                return hasAccessRight(MANAGE_DEGREE) &&
                       hasAccessRight(MANAGE_RESOURCES_STANDARD);
            }

            return hasAccessRight(MANAGE_RESOURCES_STANDARD);
        }

        return false;
    }

    private boolean userIsFromSameInstitutionAsPublication() {
        return userInstance.getOrganizationUri() != null && publication.getPublisher() != null &&
               userInstance.getOrganizationUri().equals(publication.getPublisher().getId());
    }

    private boolean userAndContributorInTheSameInstitution() {
        return publication.getEntityDescription().getContributors()
                   .stream()
                   .filter(contributor -> contributor.getIdentity() != null)
                   .flatMap(contributor ->
                                         contributor.getAffiliations().stream()
                                             .filter(Organization.class::isInstance)
                                             .map(Organization.class::cast)
                                             .map(Organization::getId))
                   .anyMatch(id ->
                                 id.equals(userInstance.getOrganizationUri()));
    }
}
