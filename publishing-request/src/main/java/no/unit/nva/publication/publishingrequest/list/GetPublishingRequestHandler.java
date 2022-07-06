package no.unit.nva.publication.publishingrequest.list;

import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.createUserInstance;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.defaultRequestService;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.parseIdentifierParameter;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.validateUserCanApprovePublishingRequest;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.storage.model.PublishingRequest;
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

        var userInfo = createUserInstance(requestInfo);
        var publicationIdentifier =
            parseIdentifierParameter(requestInfo, PUBLICATION_IDENTIFIER_PATH_PARAMETER);
        var publishingRequestIdentifier =
            parseIdentifierParameter(requestInfo,PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER);
        var query = PublishingRequest.createQuery(userInfo, publicationIdentifier,
                                                                publishingRequestIdentifier);
        var existingPublishingRequest =
            requestService.getPublishingRequest(query);
        return PublishingRequestDto.fromPublishingRequest(existingPublishingRequest);
    }



    @Override
    protected Integer getSuccessStatusCode(Void input, PublishingRequestDto output) {
        return HttpURLConnection.HTTP_OK;
    }
}
