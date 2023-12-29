package no.unit.nva.publication.permission.strategy;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.RequestInfo;

public class CuratorPermissionStrategy extends PermissionStrategy {

    @Override
    public boolean hasPermission(RequestInfo requestInfo, Publication publication) {
        if (!userIsFromSameInstitutionAsPublication(requestInfo, publication)) {
            return false;
        }
        if (isDegree(publication)) {
            return hasAccessRight(requestInfo, MANAGE_DEGREE);
        }

        return hasAccessRight(requestInfo, MANAGE_RESOURCES_STANDARD);
    }

    private static Boolean userIsFromSameInstitutionAsPublication(RequestInfo requestInfo, Publication publication) {
        return attempt(requestInfo::getCurrentCustomer)
                   .map(customer -> customer.equals(publication.getPublisher().getId()))
                   .orElse(fail -> false);
    }
}
