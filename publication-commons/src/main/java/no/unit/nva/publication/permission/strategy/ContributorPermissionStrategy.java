package no.unit.nva.publication.permission.strategy;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.role.Role.CREATOR;
import static nva.commons.core.attempt.Try.attempt;
import java.util.Objects;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.RequestInfo;

public class ContributorPermissionStrategy extends PermissionStrategy {

    @Override
    public boolean hasPermission(RequestInfo requestInfo, Publication publication) {
        var identity = attempt(requestInfo::getPersonCristinId)
                           .orElse(uriFailure -> null);

        if (identity == null) {
            return false;
        }

        return publication.getEntityDescription()
                   .getContributors()
                   .stream()
                   .filter(ContributorPermissionStrategy::isCreator)
                   .map(Contributor::getIdentity)
                   .map(Identity::getId)
                   .filter(Objects::nonNull)
                   .anyMatch(identity::equals);
    }

    private static boolean isCreator(Contributor contributor) {
        return nonNull(contributor.getRole()) && CREATOR.equals(contributor.getRole().getType());
    }
}
