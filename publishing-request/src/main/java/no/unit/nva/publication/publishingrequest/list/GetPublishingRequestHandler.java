package no.unit.nva.publication.publishingrequest.list;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.storage.model.PublishingRequest;
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

public class GetPublishingRequestHandler extends ApiGatewayHandler<Void, PublishingRequest> {

    private final PublishingRequestService requestService;

    @JacocoGenerated
    public GetPublishingRequestHandler() {
        this(defaultRequestService(), new Environment());
    }

    public GetPublishingRequestHandler(PublishingRequestService requestService, Environment environment) {
        super(Void.class, environment);
        this.requestService = requestService;
    }

    @Override
    protected PublishingRequest processInput(Void input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        var publicationIdentifier = getPublicationIdentifier(requestInfo);
        var userInstance = createUserInstance(requestInfo);
        validateUserCanApprovePublishingRequest(requestInfo);
        return requestService.getPublishingRequest(userInstance, publicationIdentifier);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PublishingRequest output) {
        return HttpURLConnection.HTTP_OK;
    }

}
