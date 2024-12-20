package no.unit.nva.publication.permission.strategy.grant;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;

public class ContributorPermissionStrategy extends GrantPermissionStrategy {

    public ContributorPermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!userIsVerifiedContributor()) {
            return false;
        }

        return switch (permission) {
            case UPDATE -> true;
            case UNPUBLISH -> isPublished() && !hasApprovedFiles();
            case PUBLISHING_REQUEST_CREATE,
                 SUPPORT_REQUEST_CREATE,
                 DOI_REQUEST_CREATE -> !isDraft();
            case UPDATE_FILES,
                 REPUBLISH,
                 DOI_REQUEST_APPROVE,
                 PUBLISHING_REQUEST_APPROVE,
                 SUPPORT_REQUEST_APPROVE,
                 TERMINATE,
                 DELETE,
                 READ_HIDDEN_FILES -> false;
        };
    }

    private boolean userIsVerifiedContributor() {
        return nonNull(this.userInstance.getPersonCristinId()) &&
               Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getContributors)
                   .stream()
                   .flatMap(List::stream)
                   .filter(this::isVerifiedContributor)
                   .anyMatch(
                       contributor -> contributor.getIdentity().getId().equals(this.userInstance.getPersonCristinId()));
    }
}
