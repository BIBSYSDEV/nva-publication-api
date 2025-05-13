package no.unit.nva.publication.permissions.publication.restrict;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationOperation.PARTIAL_UPDATE;
import static no.unit.nva.model.PublicationOperation.UPLOAD_FILE;
import static no.unit.nva.model.role.Role.SUPERVISOR;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationDenyStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public class DegreeDenyStrategy extends PublicationStrategyBase implements PublicationDenyStrategy {

    private static final boolean DENY = true;
    private static final boolean PASS = false;

    public DegreeDenyStrategy(Resource resource, UserInstance userInstance) {
        super(resource, userInstance);
    }

    @Override
    public boolean deniesAction(PublicationOperation permission) {
        if (isUsersDraft() || userInstance.isExternalClient() || userInstance.isBackendClient()) {
            return PASS;
        }

        if (isProtectedDegreeInstanceType()) {
            if (PARTIAL_UPDATE.equals(permission) || UPLOAD_FILE.equals(permission)) {
                return !userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution();
            }
            if (isProtectedDegreeInstanceTypeWithEmbargo() && !hasAccessRight(MANAGE_DEGREE_EMBARGO)) {
                return DENY;
            }
            if (hasOpenFiles()) {
                return openFileStrategy();
            } else {
                return nonOpenFileStrategy();
            }
        }

        return PASS;
    }

    private boolean openFileStrategy() {
        if (!hasAccessRight(MANAGE_DEGREE)) {
            return DENY;
        }
        if (resource.getPrioritizedClaimedPublicationChannel().isEmpty()) {
            return !userIsFromSameInstitutionAsPublicationOwner();
        } // else: ClaimedChannelDenyStrategy takes care of denying by channel claim
        return PASS;
    }

    private boolean nonOpenFileStrategy() {
        if (!userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution()) {
            return DENY;
        }
        if (!userIsFromSameInstitutionAsPublicationOwner() && userIsCuratingSupervisorsOnly()) {
            return DENY;
        }
        return PASS;
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

    private Optional<CuratingInstitution> getCuratingInstitutionsForCurrentUser() {
        return Optional.ofNullable(resource.getCuratingInstitutions())
                   .stream()
                   .flatMap(Collection::stream)
                   .filter(org -> nonNull(userInstance) && org.id().equals(userInstance.getTopLevelOrgCristinId()))
                   .findFirst();
    }

    private Optional<Contributor> getContributor(URI contributorId) {
        return resource.getEntityDescription()
                   .getContributors()
                   .stream()
                   .filter(contributor -> contributorId.equals(contributor.getIdentity().getId()))
                   .findFirst();
    }
}
