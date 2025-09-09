package no.unit.nva.publication.permissions.publication.grant;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationGrantStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public final class TrustedThirdPartyGrantStrategy extends PublicationStrategyBase implements PublicationGrantStrategy {

    public TrustedThirdPartyGrantStrategy(Resource resource, UserInstance userInstance) {
        super(resource, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        return switch (permission) {
            case UPDATE,
                 PARTIAL_UPDATE,
                 UNPUBLISH,
                 TERMINATE,
                 READ_HIDDEN_FILES,
                 UPLOAD_FILE -> canModify();
            case DELETE -> canModify() && isDraft();
            case REPUBLISH,
                 DOI_REQUEST_CREATE,
                 DOI_REQUEST_APPROVE,
                 PUBLISHING_REQUEST_CREATE,
                 APPROVE_FILES,
                 SUPPORT_REQUEST_CREATE,
                 SUPPORT_REQUEST_APPROVE -> false;
        };
    }

    private boolean canModify() {
        return nonNull(userInstance)
               && userInstance.isExternalClient()
               && attempt(
                   () -> userInstance.getTopLevelOrgCristinId().equals(resource.getResourceOwner().getOwnerAffiliation()))
                      .orElse(fail -> false);
    }
}
