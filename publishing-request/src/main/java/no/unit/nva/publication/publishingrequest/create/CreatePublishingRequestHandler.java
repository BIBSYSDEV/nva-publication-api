package no.unit.nva.publication.publishingrequest.create;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;

import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.createUserInstance;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.defaultRequestService;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.getPublicationIdentifier;

public class CreatePublishingRequestHandler extends ApiGatewayHandler<String, Void> {

    private final PublishingRequestService requestService;

    @JacocoGenerated
    public CreatePublishingRequestHandler() {
        this(defaultRequestService(), new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param requestService publicationService
     * @param environment     environment
     */
    public CreatePublishingRequestHandler(PublishingRequestService requestService,
                                          Environment environment) {
        super(String.class, environment);
        this.requestService = requestService;
    }

    @Override
    protected Void processInput(String input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        requestService.createPublishingRequest(createUserInstance(requestInfo), getPublicationIdentifier(requestInfo));
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(String input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }

}
