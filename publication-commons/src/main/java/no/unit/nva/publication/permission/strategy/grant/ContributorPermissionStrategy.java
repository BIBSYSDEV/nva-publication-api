package no.unit.nva.publication.permission.strategy.grant;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.UserInstance;

public class ContributorPermissionStrategy extends GrantPermissionStrategy {

    public ContributorPermissionStrategy(Publication publication, UserInstance userInstance, UriRetriever uriRetriever) {
        super(publication, userInstance, uriRetriever);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!userIsVerifiedContributor()) {
            return false;
        }

        return switch (permission) {
            case UPDATE, UNPUBLISH -> true;
            default -> false;
        };
    }

    private boolean userIsVerifiedContributor() {
        return nonNull(this.userInstance.getPersonCristinId()) &&
               Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getContributors)
                   .stream().flatMap(List::stream)
                   .filter(this::isVerifiedContributor)
                   .anyMatch(contributor -> contributor.getIdentity().getId().equals(this.userInstance.getPersonCristinId()));
    }

}
