package no.unit.nva.publication.utils;

import static no.unit.nva.model.PublicationOperation.REPUBLISH;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.PublicationPermissionStrategy;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public record RequestUtils(List<AccessRight> accessRights,
                           URI customerId,
                           URI customerCristinId,
                           String username,
                           URI cristinId,
                           Map<String, String> pathParameters) {

    public static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    public static final String TICKET_IDENTIFIER = "ticketIdentifier";
    public static final String MISSING_PATH_PARAM_MESSAGE = "Missing from pathParameters: %s";

    public static RequestUtils fromRequestInfo(RequestInfo requestInfo) throws UnauthorizedException {
        return new RequestUtils(requestInfo.getAccessRights(),
                                requestInfo.getCurrentCustomer(),
                                requestInfo.getTopLevelOrgCristinId().orElse(null),
                                requestInfo.getUserName(),
                                requestInfo.getPersonCristinId(),
                                requestInfo.getPathParameters());
    }

    public boolean hasOneOfAccessRights(AccessRight... rights) {
        return Arrays.stream(rights).anyMatch(accessRights::contains);
    }

    public SortableIdentifier ticketIdentifier() {
        return Optional.ofNullable(pathParameters().get(TICKET_IDENTIFIER))
                   .map(SortableIdentifier::new)
                   .orElseThrow(() -> new IllegalArgumentException(missingPathParamErrorMessage(TICKET_IDENTIFIER)));
    }

    private static String missingPathParamErrorMessage(String value) {
        return String.format(MISSING_PATH_PARAM_MESSAGE, value);
    }

    public SortableIdentifier publicationIdentifier() {
        return Optional.ofNullable(pathParameters().get(PUBLICATION_IDENTIFIER))
                   .map(SortableIdentifier::new)
                   .orElseThrow(() -> new IllegalArgumentException(missingPathParamErrorMessage(PUBLICATION_IDENTIFIER)));
    }

    public boolean hasAccessRight(AccessRight accessRight) {
        return accessRights.contains(accessRight);
    }

    public boolean isAuthorizedToManage(TicketEntry ticket, ResourceService resourceService) {
        return switch (ticket) {
            case DoiRequest doi -> hasAccessRight(MANAGE_DOI);
            case PublishingRequestCase publishing -> isAuthorizedToManagePublishingRequest(ticket, resourceService);
            case GeneralSupportRequest support -> hasAccessRight(SUPPORT);
            case UnpublishRequest unpublish -> true;
            case null, default -> false;
        };
    }

    private boolean isAuthorizedToManagePublishingRequest(TicketEntry ticket, ResourceService resourceService) {
        var publication = ticket.toPublication(resourceService);
        return PublicationPermissionStrategy.create(publication, toUserInstance()).allowsAction(REPUBLISH);
    }

    public boolean isTicketOwner(TicketEntry ticketEntry) {
        return username.equals(ticketEntry.getOwner().toString());
    }

    public UserInstance toUserInstance() {
        return UserInstance.create(username, customerId, cristinId, accessRights);
    }
}