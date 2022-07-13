package no.unit.nva.publication.publishingrequest;

import static no.unit.nva.publication.PublicationServiceConfig.defaultDynamoDbClient;
import java.time.Clock;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

//TODO: Rename or refactor class
public final class PublishingRequestUtils {

    public static final String PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER = "supportCaseIdentifier";

    private PublishingRequestUtils() {

    }

    public static UserInstance createUserInstance(RequestInfo requestInfo) throws UnauthorizedException {
        return UserInstance.create(requestInfo.getNvaUsername(), requestInfo.getCurrentCustomer());
    }

    @JacocoGenerated
    public static PublishingRequestService defaultRequestService() {
        return new PublishingRequestService(defaultDynamoDbClient(), Clock.systemDefaultZone());
    }
}
