package no.unit.nva.publication.utils;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.UserClientType;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public record RequestUtils(List<AccessRight> accessRights,
                           URI customerId,
                           URI topLevelCristinOrgId,
                           String username,
                           URI personCristinId,
                           URI personAffiliation,
                           Map<String, String> pathParameters) {

    public static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    public static final String TICKET_IDENTIFIER = "ticketIdentifier";
    public static final String MISSING_PATH_PARAM_MESSAGE = "Missing from pathParameters: %s";

    public static RequestUtils fromRequestInfo(RequestInfo requestInfo)
        throws UnauthorizedException {
        return new RequestUtils(requestInfo.getAccessRights(),
                                requestInfo.getCurrentCustomer(),
                                requestInfo.getTopLevelOrgCristinId().orElse(null),
                                requestInfo.getUserName(),
                                requestInfo.getPersonCristinId(),
                                attempt(requestInfo::getPersonAffiliation).toOptional().orElse(null),
                                requestInfo.getPathParameters());
    }

    public boolean hasOneOfAccessRights(AccessRight... rights) {
        return Arrays.stream(rights).anyMatch(accessRights::contains);
    }

    public SortableIdentifier ticketIdentifier() throws NotFoundException {
        return attempt(() -> pathParameters().get(TICKET_IDENTIFIER))
                   .map(SortableIdentifier::new)
                   .orElseThrow(failure -> new NotFoundException(missingPathParamErrorMessage(TICKET_IDENTIFIER)));
    }

    private static String missingPathParamErrorMessage(String value) {
        return String.format(MISSING_PATH_PARAM_MESSAGE, value);
    }

    public SortableIdentifier publicationIdentifier() throws NotFoundException {
        return attempt(() -> pathParameters().get(PUBLICATION_IDENTIFIER))
                   .map(SortableIdentifier::new)
                   .orElseThrow(failure -> new NotFoundException(
                       missingPathParamErrorMessage(PUBLICATION_IDENTIFIER)));
    }

    public boolean hasAccessRight(AccessRight accessRight) {
        return accessRights.contains(accessRight);
    }

    public boolean isAuthorizedToManage(TicketEntry ticket) {
        return switch (ticket) {
            case DoiRequest doi -> hasAccessRight(MANAGE_DOI);
            case PublishingRequestCase publishing -> hasAccessRight(MANAGE_PUBLISHING_REQUESTS);
            case FilesApprovalThesis thesis -> hasAccessRight(MANAGE_DEGREE);
            case GeneralSupportRequest support -> hasAccessRight(SUPPORT);
            case UnpublishRequest unpublish -> true;
            case null, default -> false;
        };
    }

    public boolean isTicketOwner(TicketEntry ticketEntry) {
        return username.equals(ticketEntry.getOwner().toString());
    }

    public UserInstance toUserInstance() {
        return new UserInstance(username, customerId, topLevelCristinOrgId, personAffiliation, personCristinId,
                                accessRights,
                                UserClientType.INTERNAL, null);
    }
}