package no.unit.nva.publication.permission.strategy.grant;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.role.Role.CREATOR;
import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.UserInstance;

public class ContributorPermissionStrategy extends GrantPermissionStrategy {

    public ContributorPermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        return switch (permission) {
            case UPDATE, UNPUBLISH -> canModify();
            default -> false;
        };
    }

    private boolean canModify() {
        return userIsVerifiedContributor();
    }

    private boolean userIsVerifiedContributor() {
        return nonNull(this.userInstance.getPersonCristinId()) &&
               Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getContributors)
                   .stream().flatMap(List::stream)
                   .filter(ContributorPermissionStrategy::isCreator)
                   .map(Contributor::getIdentity)
                   .map(Identity::getId)
                   .filter(Objects::nonNull)
                   .anyMatch(a -> a.equals(this.userInstance.getPersonCristinId()));
    }

    private static boolean isCreator(Contributor contributor) {
        return attempt(contributor::getRole)
                   .map(RoleType::getType)
                   .map(CREATOR::equals)
                   .get();
    }
}
