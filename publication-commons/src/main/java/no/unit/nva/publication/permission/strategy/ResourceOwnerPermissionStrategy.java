package no.unit.nva.publication.permission.strategy;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.RequestInfo;

public class ResourceOwnerPermissionStrategy extends PermissionStrategy {

    @Override
    public boolean hasPermission(RequestInfo requestInfo, Publication publication) {
        if (isDegree(publication)) {
            return false;
        }
        return attempt(requestInfo::getUserName)
                           .map(username -> UserInstance.fromPublication(publication).getUsername().equals(username))
                           .orElse(fail -> false);
    }
}
