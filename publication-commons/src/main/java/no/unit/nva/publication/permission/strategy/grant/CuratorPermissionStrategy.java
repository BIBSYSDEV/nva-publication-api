package no.unit.nva.publication.permission.strategy.grant;

import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;

public class CuratorPermissionStrategy extends GrantPermissionStrategy {

    public CuratorPermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!canManage()) {
            return false;
        }

        return switch (permission) {
            case UPDATE -> true;
            case REPUBLISH -> isUnpublished() || isDraft();
            case UNPUBLISH -> isPublished();
            default -> false;
        };
    }

    private boolean canManage() {
        return (userIsFromSameInstitutionAsPublication() || userAndContributorInTheSameInstitution())
               && hasAccessRight(MANAGE_RESOURCES_STANDARD);
    }

    private boolean userIsFromSameInstitutionAsPublication() {
        return userInstance.getCustomerId() != null && publication.getPublisher() != null &&
               userInstance.getCustomerId().equals(publication.getPublisher().getId());
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
                                 id.equals(userInstance.getCustomerId()));
    }
}
