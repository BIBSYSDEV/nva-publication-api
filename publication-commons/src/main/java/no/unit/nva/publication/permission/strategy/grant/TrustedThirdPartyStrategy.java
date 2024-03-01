package no.unit.nva.publication.permission.strategy.grant;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.core.JacocoGenerated;

public class TrustedThirdPartyStrategy extends GrantPermissionStrategy {
    // we should make sure all external users gets the claims they need and remove this class
    public TrustedThirdPartyStrategy(Publication publication, UserInstance userInstance, UriRetriever uriRetriever) {
        super(publication, userInstance, uriRetriever);
    }

    @JacocoGenerated
    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!userInstance.isExternalClient() || !userMatchesResourceOwner()) {
            return false;
        }

        return switch (permission) {
            case UPDATE, UNPUBLISH, TERMINATE -> true;
            case DELETE -> isDraft();
            default -> false;
        };
    }

    @JacocoGenerated
    private boolean userMatchesResourceOwner() {
        return attempt(() -> userInstance.getTopLevelOrgCristinId().equals(publication.getResourceOwner().getOwnerAffiliation()))
                   .orElse(fail -> false);
    }
}
