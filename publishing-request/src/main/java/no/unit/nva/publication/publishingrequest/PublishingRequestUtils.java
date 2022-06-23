package no.unit.nva.publication.publishingrequest;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;

import java.time.Clock;

import static nva.commons.apigateway.AccessRight.APPROVE_PUBLISH_REQUEST;
import static nva.commons.core.attempt.Try.attempt;

public class PublishingRequestUtils {

    public static final String API_PUBLICATION_PATH_IDENTIFIER = "publicationIdentifier";
    public static final String INVALID_PUBLICATION_ID_ERROR = "Invalid publication id: ";


    public static SortableIdentifier getPublicationIdentifier(RequestInfo requestInfo) throws BadRequestException {
        var publicationIdentifierString = requestInfo.getPathParameter(API_PUBLICATION_PATH_IDENTIFIER);
        return attempt(() -> new SortableIdentifier(publicationIdentifierString))
                .orElseThrow(
                        fail -> new BadRequestException(INVALID_PUBLICATION_ID_ERROR + publicationIdentifierString));
    }

    public static UserInstance createUserInstance(RequestInfo requestInfo) throws UnauthorizedException {
        return UserInstance.create(requestInfo.getNvaUsername(), requestInfo.getCurrentCustomer());
    }

    private static boolean userIsNotAuthorized(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(APPROVE_PUBLISH_REQUEST.toString());
    }

    public static void validateUserCanApprovePublishingRequest(RequestInfo requestInfo) throws NotAuthorizedException {
        if (userIsNotAuthorized(requestInfo)) {
            throw new NotAuthorizedException();
        }
    }

    public static PublishingRequestService defaultRequestService() {
        return new PublishingRequestService(AmazonDynamoDBClientBuilder.defaultClient(),  Clock.systemDefaultZone());
    }
}
