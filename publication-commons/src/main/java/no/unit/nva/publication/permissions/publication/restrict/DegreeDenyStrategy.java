package no.unit.nva.publication.permissions.publication.restrict;

import static java.util.Objects.nonNull;
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
import no.unit.nva.publication.permissions.publication.PublicationDenyStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public class DegreeDenyStrategy extends PublicationStrategyBase implements PublicationDenyStrategy {

    public DegreeDenyStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
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
        return nonNull(userInstance) && userInstance.getTopLevelOrgCristinId()
                                            .equals(publication.getResourceOwner().getOwnerAffiliation());
    }

    private Optional<CuratingInstitution> getCuratingInstitutionsForCurrentUser() {
        return Optional.ofNullable(publication.getCuratingInstitutions())
                   .stream()
                   .flatMap(Collection::stream)
                   .filter(org -> nonNull(userInstance) && org.id().equals(userInstance.getTopLevelOrgCristinId()))
                   .findFirst();
    }

    private Optional<Contributor> getContributor(URI contributorId) {
        return publication.getEntityDescription()
                   .getContributors()
                   .stream()
                   .filter(contributor -> contributorId.equals(contributor.getIdentity().getId()))
                   .findFirst();
    }

    private boolean isUsersDraft() {
        return isDraft() && isOwner();
    }
}
