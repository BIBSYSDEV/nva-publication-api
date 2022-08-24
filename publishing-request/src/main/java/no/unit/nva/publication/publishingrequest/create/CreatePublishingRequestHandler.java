package no.unit.nva.publication.publishingrequest.create;

import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.publishingrequest.TicketUtils.createUserInstance;
import static no.unit.nva.publication.publishingrequest.TicketUtils.defaultRequestService;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.publishingrequest.PublishingRequestCaseDto;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.JacocoGenerated;

public class CreatePublishingRequestHandler extends
                                            ApiGatewayHandler<PublishingRequestOpenCase, PublishingRequestCaseDto> {
    
    private final TicketService requestService;
    
    @JacocoGenerated
    public CreatePublishingRequestHandler() {
        this(defaultRequestService());
    }
    
    public CreatePublishingRequestHandler(TicketService requestService) {
        super(PublishingRequestOpenCase.class);
        this.requestService = requestService;
    }
    
    @Override
    protected PublishingRequestCaseDto processInput(PublishingRequestOpenCase input,
                                                    RequestInfo requestInfo,
                                                    Context context) throws ApiGatewayException {
        final var userInstance = createUserInstance(requestInfo);
        final var publicationIdentifier =
            new SortableIdentifier(requestInfo.getPathParameter(PUBLICATION_IDENTIFIER_PATH_PARAMETER));
    
        var publishingRequest = PublishingRequestCase.createOpeningCaseObject(userInstance, publicationIdentifier);
        var newPublishingRequest =
            attempt(() -> requestService.createTicket(publishingRequest, PublishingRequestCase.class))
                .orElseThrow(fail -> handleErrors(fail.getException()));
    
        var persistedRequest = requestService.fetchTicket(newPublishingRequest);
        return PublishingRequestCaseDto.createResponseObject((PublishingRequestCase) persistedRequest);
    }
    
    @Override
    protected Integer getSuccessStatusCode(PublishingRequestOpenCase input,
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
