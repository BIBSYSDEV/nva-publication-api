package no.unit.nva.publication.permission.strategy.grant;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;

public class TrustedThirdPartyStrategy extends GrantPermissionStrategy {

    public TrustedThirdPartyStrategy(Publication publication,
                                     UserInstance userInstance,
                                     ResourceService resourceService) {
        super(publication, userInstance, resourceService);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        return switch (permission) {
            case UPDATE, UNPUBLISH, TICKET_PUBLISH, TERMINATE -> canModify();
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
        return userInstance.isExternalClient()
               && attempt(
                   () -> userInstance.getCustomerId().equals(publication.getPublisher().getId()))
                      .orElse(fail -> false);
    }
}
