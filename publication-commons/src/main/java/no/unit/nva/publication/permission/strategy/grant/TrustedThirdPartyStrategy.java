package no.unit.nva.publication.permission.strategy.grant;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.UserInstance;

public class TrustedThirdPartyStrategy extends GrantPermissionStrategy {
    // we should make sure all external users gets the claims they need and remove this class
    public TrustedThirdPartyStrategy(Publication publication, UserInstance userInstance, UriRetriever uriRetriever) {
        super(publication, userInstance, uriRetriever);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!userInstance.isExternalClient() || !userMatchesPublisher()) {
            return false;
        }

        return switch (permission) {
            case UPDATE, UNPUBLISH, TICKET_PUBLISH, TERMINATE -> true;
            case DELETE -> isDraft();
            default -> false;
        };
    }

    private boolean userMatchesPublisher() {
        return attempt(() -> userInstance.getCustomerId().equals(publication.getPublisher().getId()))
                   .orElse(fail -> false);
    }
}
