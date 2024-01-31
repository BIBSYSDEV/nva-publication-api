package no.unit.nva.publication.permission.strategy;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.RequestInfo;

public class TrustedThirdPartyStrategy extends PermissionStrategy {

    private final UserInstance userInstance;

    public TrustedThirdPartyStrategy(UserInstance userInstance) {
        this.userInstance = userInstance;
    }

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
        return requestInfo.clientIsThirdParty() &&
               attempt(
                   () -> userInstance.getOrganizationUri().equals(publication.getPublisher().getId()))
                   .orElse(fail -> false);
    }
}
