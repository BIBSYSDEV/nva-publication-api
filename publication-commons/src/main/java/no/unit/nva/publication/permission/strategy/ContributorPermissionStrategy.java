package no.unit.nva.publication.permission.strategy;

import static no.unit.nva.model.role.Role.CREATOR;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.role.RoleType;
import nva.commons.apigateway.RequestInfo;

public class ContributorPermissionStrategy extends PermissionStrategy {

    @Override
    public boolean hasPermissionToUpdate(RequestInfo requestInfo, Publication publication) {
        return canModify(requestInfo, publication);
    }

    @Override
    public boolean hasPermissionToDelete(RequestInfo requestInfo, Publication publication) {
        return false;
    }

    @Override
    public boolean hasPermissionToUnpublish(RequestInfo requestInfo, Publication publication) {
        return canModify(requestInfo, publication);
    }

    private boolean canModify(RequestInfo requestInfo, Publication publication) {
        if (isDegree(publication)) {
            return false;
        }
        return attempt(requestInfo::getPersonCristinId)
                   .map(cristinId -> publicationContainsContributor(cristinId, publication))
                   .orElse(fail -> false);
    }

    private boolean publicationContainsContributor(URI contributor, Publication publication) {
        return publication.getEntityDescription()
                   .getContributors()
                   .stream()
                   .filter(ContributorPermissionStrategy::isCreator)
                   .map(Contributor::getIdentity)
                   .map(Identity::getId)
                   .filter(Objects::nonNull)
                   .anyMatch(contributor::equals);
    }

    private static boolean isCreator(Contributor contributor) {
        return attempt(contributor::getRole)
                   .map(RoleType::getType)
                   .map(CREATOR::equals)
                   .get();
    }
}
