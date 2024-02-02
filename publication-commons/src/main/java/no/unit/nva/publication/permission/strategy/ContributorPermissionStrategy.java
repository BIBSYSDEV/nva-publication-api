package no.unit.nva.publication.permission.strategy;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.role.Role.CREATOR;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;

public class ContributorPermissionStrategy extends PermissionStrategy {

    public ContributorPermissionStrategy(Publication publication, UserInstance userInstance, List<AccessRight> accessRights,
                                     URI personCristinId) {
        super(publication, userInstance, accessRights, personCristinId);
    }

    @Override
    public boolean hasPermissionToUpdate() {
        return canModify();
    }

    @Override
    public boolean hasPermissionToDelete() {
        return false;
    }

    @Override
    public boolean hasPermissionToUnpublish() {
        return canModify();
    }

    private boolean canModify() {
        if (isDegree()) {
            return false;
        }

        return userIsVerifiedContributor();
    }

    private boolean userIsVerifiedContributor() {
        return nonNull(personCristinId) && publication.getEntityDescription()
                                               .getContributors()
                                               .stream()
                                               .filter(ContributorPermissionStrategy::isCreator)
                                               .map(Contributor::getIdentity)
                                               .map(Identity::getId)
                                               .filter(Objects::nonNull)
                                               .anyMatch(personCristinId::equals);
    }

    private static boolean isCreator(Contributor contributor) {
        return attempt(contributor::getRole)
                   .map(RoleType::getType)
                   .map(CREATOR::equals)
                   .get();
    }
}
