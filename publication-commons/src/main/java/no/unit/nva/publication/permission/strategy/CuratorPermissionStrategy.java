package no.unit.nva.publication.permission.strategy;

import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_RESOURCES;
import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.RequestInfo;

public class CuratorPermissionStrategy extends PermissionStrategy {

    @Override
    public boolean hasPermission(RequestInfo requestInfo, Publication publication) {
        if (!userIsFromSameInstitutionAsPublication(requestInfo, publication)) {
            return false;
        }

        return hasAccessRight(requestInfo, EDIT_OWN_INSTITUTION_RESOURCES);
    }

    private static Boolean userIsFromSameInstitutionAsPublication(RequestInfo requestInfo, Publication publication) {
        var requestInfoCurrentCustomer = attempt(requestInfo::getCurrentCustomer)
                                             .orElse(uriFailure -> null);

        if (requestInfoCurrentCustomer == null) {
            return false;
        }

        return publication.getPublisher().getId().equals(requestInfoCurrentCustomer);
    }
}
