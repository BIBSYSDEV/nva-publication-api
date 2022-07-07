package no.unit.nva.publication.publishingrequest;

import static no.unit.nva.publication.PublicationServiceConfig.defaultDynamoDbClient;
import static nva.commons.apigateway.AccessRight.APPROVE_PUBLISH_REQUEST;
import static nva.commons.core.attempt.Try.attempt;
import java.time.Clock;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

public final class PublishingRequestUtils {

    public static final String PUBLICATION_IDENTIFIER_PATH_PARAMETER = "publicationIdentifier";
    public static final String PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER = "publishingRequestIdentifier";
    public static final String ILLEGAL_IDENTIFIER_ERROR = "Illegal identifier";

    private PublishingRequestUtils() {

    }

    public static SortableIdentifier parseIdentifierParameter(RequestInfo requestInfo,
                                                              String publicationIdentifierPathParameter)
        throws NotFoundException {
        return attempt(() -> requestInfo.getPathParameter(publicationIdentifierPathParameter))
            .map(SortableIdentifier::new)
            .orElseThrow(fail -> new NotFoundException(ILLEGAL_IDENTIFIER_ERROR));
    }

    public static UserInstance createUserInstance(RequestInfo requestInfo) throws UnauthorizedException {
        return UserInstance.create(requestInfo.getNvaUsername(), requestInfo.getCurrentCustomer());
    }

    public static void validateUserCanApprovePublishingRequest(RequestInfo requestInfo) throws NotAuthorizedException {
        if (userIsNotAuthorized(requestInfo)) {
            throw new NotAuthorizedException();
        }
    }

    @JacocoGenerated
    public static PublishingRequestService defaultRequestService() {
        return new PublishingRequestService(defaultDynamoDbClient(), Clock.systemDefaultZone());
    }

    private static boolean userIsNotAuthorized(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(APPROVE_PUBLISH_REQUEST.toString());
    }
}
