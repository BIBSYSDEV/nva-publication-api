package no.unit.nva.publication.permission.strategy;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.RequestInfo;

public class CuratorPermissionStrategy extends PermissionStrategy {

    private final UserInstance userInstance;

    public CuratorPermissionStrategy(UserInstance userInstance) {
        this.userInstance = userInstance;
    }

    @Override
    public boolean hasPermissionToDelete(RequestInfo requestInfo, Publication publication) {
        return canManage(requestInfo, publication);
    }

    @Override
    public boolean hasPermissionToUnpublish(RequestInfo requestInfo, Publication publication) {
        return canManage(requestInfo, publication);
    }

    @Override
    public boolean hasPermissionToUpdate(RequestInfo requestInfo, Publication publication) {
        return canManage(requestInfo, publication);
    }

    private boolean canManage(RequestInfo requestInfo, Publication publication) {
        if (userIsFromSameInstitutionAsPublication(requestInfo, publication) ||
            userAndContributorInTheSameInstitution(publication, userInstance)) {
            if (isDegree(publication)) {
                return hasAccessRight(requestInfo, MANAGE_DEGREE) &&
                       hasAccessRight(requestInfo, MANAGE_RESOURCES_STANDARD);
            }

            return hasAccessRight(requestInfo, MANAGE_RESOURCES_STANDARD);
        }

        return false;
    }

    private static Boolean userIsFromSameInstitutionAsPublication(RequestInfo requestInfo, Publication publication) {
        return attempt(requestInfo::getCurrentCustomer)
                   .map(customer -> customer.equals(publication.getPublisher().getId()))
                   .orElse(fail -> false);
    }

    private static boolean userAndContributorInTheSameInstitution(Publication publication, UserInstance userInstance) {
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
