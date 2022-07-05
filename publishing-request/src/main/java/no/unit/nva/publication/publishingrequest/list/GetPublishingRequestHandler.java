package no.unit.nva.publication.publishingrequest.list;

import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.createUserInstance;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.defaultRequestService;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.getPublicationIdentifier;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.validateUserCanApprovePublishingRequest;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class GetPublishingRequestHandler extends ApiGatewayHandler<Void, PublishingRequestDto> {

    private final PublishingRequestService requestService;

    @JacocoGenerated
    public GetPublishingRequestHandler() {
        this(defaultRequestService());
    }

    public GetPublishingRequestHandler(PublishingRequestService requestService) {
        super(Void.class);
        this.requestService = requestService;
    }

    @Override
    protected PublishingRequestDto processInput(Void input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        validateUserCanApprovePublishingRequest(requestInfo);

        var existingPublishingRequest =
            requestService.getPublishingRequest(createUserInstance(requestInfo),getPublicationIdentifier(requestInfo));
        return PublishingRequestDto.fromPublishingRequest(existingPublishingRequest);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PublishingRequestDto output) {
        return HttpURLConnection.HTTP_OK;
    }

}
