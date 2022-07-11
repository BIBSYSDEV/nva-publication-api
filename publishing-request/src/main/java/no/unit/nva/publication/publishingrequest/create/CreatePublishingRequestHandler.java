package no.unit.nva.publication.publishingrequest.create;

import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.createUserInstance;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.defaultRequestService;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.storage.model.PublishingRequestCase;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.JacocoGenerated;

public class CreatePublishingRequestHandler extends
                                            ApiGatewayHandler<NewPublishingRequest, PublishingRequestCaseDto> {

    private final PublishingRequestService requestService;

    @JacocoGenerated
    public CreatePublishingRequestHandler() {
        this(defaultRequestService());
    }

    public CreatePublishingRequestHandler(PublishingRequestService requestService) {
        super(NewPublishingRequest.class);
        this.requestService = requestService;
    }

    @Override
    protected PublishingRequestCaseDto processInput(
        NewPublishingRequest input,
        RequestInfo requestInfo,
        Context context) throws ApiGatewayException {
        final var userInstance = createUserInstance(requestInfo);
        final var publicationIdentifier =
            new SortableIdentifier(requestInfo.getPathParameter(PUBLICATION_IDENTIFIER_PATH_PARAMETER));

        var publishingRequest = PublishingRequestCase.createOpeningCaseObject(userInstance, publicationIdentifier);
        var newPublishingRequest =
            attempt(() -> requestService.createPublishingRequest(publishingRequest))
                .orElseThrow(fail -> handleErrors(fail.getException()));

        var persistedRequest = requestService.getPublishingRequest(newPublishingRequest);
        return PublishingRequestCaseDto.create(persistedRequest);
    }

    @Override
    protected Integer getSuccessStatusCode(NewPublishingRequest input,
                                           PublishingRequestCaseDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    private ApiGatewayException handleErrors(Exception exception) {
        if (exception instanceof TransactionFailedException) {
            return new ConflictException(exception, exception.getMessage());
        } else if (exception instanceof ApiGatewayException) {
            return (ApiGatewayException) exception;
        } else if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        } else {
            throw new RuntimeException(exception);
        }
    }
}
