package no.unit.nva.publication.ticket.utils;

import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public record RequestUtils(List<AccessRight> accessRights, URI customer) {

    public static RequestUtils fromRequestInfo(RequestInfo requestInfo) throws UnauthorizedException {
        return new RequestUtils(requestInfo.getAccessRights(), requestInfo.getCurrentCustomer());
    }

    public boolean hasOneOfAccessRights(AccessRight... rights) {
        return Arrays.stream(rights).anyMatch(accessRights::contains);
    }

    public boolean hasAccessRight(AccessRight accessRight) {
        return accessRights.contains(accessRight);
    }

    public boolean isAuthorizedToManage(TicketEntry ticket) {
        return switch (ticket) {
            case DoiRequest doi -> hasAccessRight(MANAGE_DOI);
            case PublishingRequestCase publishing -> hasAccessRight(MANAGE_PUBLISHING_REQUESTS);
            case GeneralSupportRequest support -> hasAccessRight(SUPPORT);
            case null, default -> false;
        };
    }
}
