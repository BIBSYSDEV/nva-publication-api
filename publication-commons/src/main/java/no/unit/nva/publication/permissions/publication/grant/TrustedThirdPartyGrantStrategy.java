package no.unit.nva.publication.permissions.publication.grant;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationGrantStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public final class TrustedThirdPartyGrantStrategy extends PublicationStrategyBase implements PublicationGrantStrategy {

    public TrustedThirdPartyGrantStrategy(Publication publication,
                                          UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        return switch (permission) {
            case UPDATE,
                 UNPUBLISH,
                 TERMINATE,
                 READ_HIDDEN_FILES -> canModify();
            case DELETE -> canModify() && isDraft();
            case UPDATE_FILES,
                 REPUBLISH,
                 DOI_REQUEST_CREATE,
                 DOI_REQUEST_APPROVE,
                 PUBLISHING_REQUEST_CREATE,
                 PUBLISHING_REQUEST_APPROVE,
                 SUPPORT_REQUEST_CREATE,
                 SUPPORT_REQUEST_APPROVE -> false;
        };
    }

    private boolean canModify() {
        if (isNull(userInstance)) {
            return false;
        }

        return userInstance.isExternalClient()
               && attempt(
                   () -> userInstance.getCustomerId().equals(publication.getPublisher().getId()))
                      .orElse(fail -> false);
    }
}
