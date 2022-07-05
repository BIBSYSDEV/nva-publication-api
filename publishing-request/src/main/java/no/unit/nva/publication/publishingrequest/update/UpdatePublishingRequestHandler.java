package no.unit.nva.publication.publishingrequest.update;

import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.createUserInstance;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.defaultRequestService;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.getPublicationIdentifier;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.validateUserCanApprovePublishingRequest;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.publishingrequest.UpdatePublishingRequest;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class UpdatePublishingRequestHandler extends ApiGatewayHandler<UpdatePublishingRequest, PublishingRequest> {

    private final PublishingRequestService requestService;


    @SuppressWarnings("unused")
    @JacocoGenerated
    public UpdatePublishingRequestHandler() {
        this(defaultRequestService());
    }

    public UpdatePublishingRequestHandler(PublishingRequestService requestService) {
        super(UpdatePublishingRequest.class);
        this.requestService = requestService;
    }

    @Override
    protected PublishingRequest processInput(UpdatePublishingRequest input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        input.validate();
        validateUserCanApprovePublishingRequest(requestInfo);
        return updatePublishingRequestStatus(
                createUserInstance(requestInfo),
                input.getPublishingRequestStatus(),
                getPublicationIdentifier(requestInfo));
    }

    @Override
    protected Integer getSuccessStatusCode(UpdatePublishingRequest input, PublishingRequest output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }

    private PublishingRequest updatePublishingRequestStatus(UserInstance userInstance,
                                               PublishingRequestStatus publishingRequestStatus,
                                               SortableIdentifier publicationIdentifier) throws ApiGatewayException {
        requestService.updatePublishingRequest(userInstance, publicationIdentifier, publishingRequestStatus);
        return requestService.getPublishingRequest(userInstance, publicationIdentifier);
    }

}
