package no.unit.nva.publication.permission.strategy.restrict;

import static no.unit.nva.model.role.Role.SUPERVISOR;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;

public class NonDegreePermissionStrategy extends DenyPermissionStrategy {

    public NonDegreePermissionStrategy(Publication publication, UserInstance userInstance,
                                       ResourceService resourceService) {
        super(publication, userInstance, resourceService);
    }

    @Override
    public boolean deniesAction(PublicationOperation permission) {
        if (isUsersDraft() || userInstance.isExternalClient() || userInstance.isBackendClient()) {
            return false; // allow
        }

        if (isProtectedDegreeInstanceType()) {
            if (!hasAccessRight(MANAGE_DEGREE)) {
                return true; // deny
            }
            if (isProtectedDegreeInstanceTypeWithEmbargo() && !hasAccessRight(MANAGE_DEGREE_EMBARGO)) {
                return true; // deny
            }
            if (!userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution()) {
                return true; // deny
            }
            return !currentUserHaveSameTopLevelAsOwner() && userIsCuratingSupervisorsOnly(); // deny
        }

        return false; // allow
    }

    private boolean userIsCuratingSupervisorsOnly() {
        var roles = getCuratingInstitutionsForCurrentUser().map(CuratingInstitution::contributorCristinIds)
                                 .stream()
                                 .flatMap(Collection::stream)
                                 .map(this::getContributor)
                                 .filter(Optional::isPresent)
                                 .map(Optional::get)
                                 .map(Contributor::getRole)
                                 .map(RoleType::getType)
                                 .toList();

        return !roles.isEmpty() && roles.stream().allMatch(role -> role.equals(SUPERVISOR));
    }

    private boolean currentUserHaveSameTopLevelAsOwner() {
        return userInstance.getTopLevelOrgCristinId().equals(publication.getResourceOwner().getOwnerAffiliation());
    }

    private Optional<CuratingInstitution> getCuratingInstitutionsForCurrentUser() {
        return Optional.ofNullable(publication.getCuratingInstitutions())
                   .stream()
                   .flatMap(Collection::stream)
                   .filter(org -> org.id().equals(userInstance.getTopLevelOrgCristinId()))
                   .findFirst();
    }

    private Optional<Contributor> getContributor(URI contributorId) {
        return publication.getEntityDescription()
                   .getContributors()
                   .stream()
                   .filter(contributor -> contributor.getIdentity().getId().equals(contributorId))
                   .findFirst();
    }

    private boolean isUsersDraft() {
        return isDraft() && isOwner();
    }
}
