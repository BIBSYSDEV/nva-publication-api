package no.unit.nva.publication.permissionStrategy;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissionStrategy.PermissionStrategy;
import nva.commons.apigateway.RequestInfo;

public class ResourceOwnerPermissionStrategy extends PermissionStrategy {

    @Override
    public boolean hasPermission(RequestInfo requestInfo, Publication publication) {
        var username = attempt(requestInfo::getUserName)
                           .orElse(stringFailure -> null);

        if (username == null) {
            return false;
        }

        return UserInstance.fromPublication(publication).getUsername().equals(username);
    }
}
