package no.unit.nva.publication.permission.strategy;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import java.net.URI;
import java.util.List;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;

public class CuratorPermissionStrategy extends PermissionStrategy {

    public CuratorPermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean hasPermissionToDelete() {
        return canManage();
    }

    @Override
    public boolean hasPermissionToUnpublish() {
        return canManage();
    }

    @Override
    public boolean hasPermissionToUpdate() {
        return canManage();
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
        return userInstance.getOrganizationUri().equals(publication.getPublisher().getId());
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
