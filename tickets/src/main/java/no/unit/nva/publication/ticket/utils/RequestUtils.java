package no.unit.nva.publication.ticket.utils;

import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public record RequestUtils(List<AccessRight> accessRights,
                           URI customer,
                           String username,
                           Map<String, String> pathParameters) {

    public static RequestUtils fromRequestInfo(RequestInfo requestInfo) throws UnauthorizedException {
        return new RequestUtils(requestInfo.getAccessRights(),
                                requestInfo.getCurrentCustomer(),
                                requestInfo.getUserName(),
                                requestInfo.getPathParameters());
    }

    public boolean hasOneOfAccessRights(AccessRight... rights) {
        return Arrays.stream(rights).anyMatch(accessRights::contains);
    }

    public SortableIdentifier pathParameterAsIdentifier(String parameter) {
        return Optional.ofNullable(pathParameters().get(parameter))
                   .map(SortableIdentifier::new)
                   .orElseThrow(() -> new IllegalArgumentException("Missing from pathParameters: " + parameter));
    }

    public boolean hasAccessRight(AccessRight accessRight) {
        return accessRights.contains(accessRight);
    }

    public boolean isAuthorizedToManage(TicketEntry ticket) {
        return switch (ticket) {
            case DoiRequest doi -> hasAccessRight(MANAGE_DOI);
            case PublishingRequestCase publishing -> hasAccessRight(MANAGE_PUBLISHING_REQUESTS);
            case GeneralSupportRequest support -> hasAccessRight(SUPPORT);
            case UnpublishRequest unpublish -> true;
            case null, default -> false;
        };
    }
}
