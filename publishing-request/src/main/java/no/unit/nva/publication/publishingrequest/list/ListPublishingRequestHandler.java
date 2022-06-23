package no.unit.nva.publication.publishingrequest.list;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.publication.publishingrequest.SearchResponse;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.storage.model.PublishingRequest;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import java.net.URI;

import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.createUserInstance;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.defaultRequestService;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.validateUserCanApprovePublishingRequest;
import static nva.commons.core.attempt.Try.attempt;

public class ListPublishingRequestHandler extends ApiGatewayHandler<Void, SearchResponse<PublishingRequest>> {

    private final PublishingRequestService requestService;

    @JacocoGenerated
    public ListPublishingRequestHandler() {
        this(defaultRequestService(), new Environment());
    }

    public ListPublishingRequestHandler(PublishingRequestService requestService, Environment environment) {
        super(Void.class, environment);
        this.requestService = requestService;
    }

    @Override
    protected SearchResponse<PublishingRequest> processInput(Void input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        validateUserCanApprovePublishingRequest(requestInfo);
        var hits = requestService.listPublishingRequestsForUser(createUserInstance(requestInfo));
        return new SearchResponse.Builder().withId(getRequestUri(requestInfo)).withHits(hits).build();

    }

    @JacocoGenerated
    URI getRequestUri(RequestInfo requestInfo) {
        var requestUri = attempt(() -> requestInfo.getRequestUri()).toOptional();
        return requestUri.isPresent() ? requestUri.get() : URI.create("localhost");
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, SearchResponse<PublishingRequest> output) {
        return HttpURLConnection.HTTP_OK;
    }
}
