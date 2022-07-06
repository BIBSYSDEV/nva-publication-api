package no.unit.nva.publication.publishingrequest;

import static nva.commons.apigateway.AccessRight.APPROVE_PUBLISH_REQUEST;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import java.time.Clock;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public final class PublishingRequestUtils {

    public static final String PUBLICATION_IDENTIFIER_PATH_PARAMETER = "publicationIdentifier";
    public static final String PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER = "publishingRequestIdentifier";

    private PublishingRequestUtils() {

    }

    public static UserInstance createUserInstance(RequestInfo requestInfo) throws UnauthorizedException {
        return UserInstance.create(requestInfo.getNvaUsername(), requestInfo.getCurrentCustomer());
    }

    public static void validateUserCanApprovePublishingRequest(RequestInfo requestInfo) throws NotAuthorizedException {
        if (userIsNotAuthorized(requestInfo)) {
            throw new NotAuthorizedException();
        }
    }

    public static PublishingRequestService defaultRequestService() {
        return new PublishingRequestService(AmazonDynamoDBClientBuilder.defaultClient(), Clock.systemDefaultZone());
    }

    private static boolean userIsNotAuthorized(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(APPROVE_PUBLISH_REQUEST.toString());
    }
}
