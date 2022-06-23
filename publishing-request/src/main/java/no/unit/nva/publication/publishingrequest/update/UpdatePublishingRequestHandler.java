package no.unit.nva.publication.publishingrequest.update;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.publishingrequest.ApiUpdatePublishingRequest;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;

import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.createUserInstance;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.defaultRequestService;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.getPublicationIdentifier;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.validateUserCanApprovePublishingRequest;

public class UpdatePublishingRequestHandler extends ApiGatewayHandler<ApiUpdatePublishingRequest, Void> {

    private final PublishingRequestService requestService;


    @SuppressWarnings("unused")
    @JacocoGenerated
    public UpdatePublishingRequestHandler() {
        this(defaultRequestService(), new Environment());
    }

    public UpdatePublishingRequestHandler(PublishingRequestService requestService, Environment environment) {
        super(ApiUpdatePublishingRequest.class, environment);
        this.requestService = requestService;
    }

    @Override
    protected Void processInput(ApiUpdatePublishingRequest input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        input.validate();
        validateUserCanApprovePublishingRequest(requestInfo);
        updatePublishingRequestStatus(
                createUserInstance(requestInfo),
                input.getPublishingRequestStatus(),
                getPublicationIdentifier(requestInfo));
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(ApiUpdatePublishingRequest input, Void output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }

    private void updatePublishingRequestStatus(UserInstance userInstance,
                                               PublishingRequestStatus publishingRequestStatus,
                                               SortableIdentifier publicationIdentifier) throws ApiGatewayException {
        requestService.updatePublishingRequest(userInstance, publicationIdentifier, publishingRequestStatus);
    }

}
