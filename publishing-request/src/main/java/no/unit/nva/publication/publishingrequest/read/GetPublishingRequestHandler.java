package no.unit.nva.publication.publishingrequest.read;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.publishingrequest.TicketUtils.TICKET_IDENTIFIER_PATH_PARAMETER;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.publishingrequest.PublishingRequestCaseDto;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

public class GetPublishingRequestHandler extends ApiGatewayHandler<Void, PublishingRequestCaseDto> {
    
    private final TicketService ticketService;
    
    @JacocoGenerated
    public GetPublishingRequestHandler() {
        this(new TicketService(DEFAULT_DYNAMODB_CLIENT));
    }
    
    protected GetPublishingRequestHandler(TicketService ticketService) {
        super(Void.class);
        this.ticketService = ticketService;
    }
    
    @Override
    protected PublishingRequestCaseDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var persistedRequest = (PublishingRequestCase) fetchPublishingRequest(requestInfo);
        return PublishingRequestCaseDto.createResponseObject(persistedRequest);
    }
    
    @Override
    protected Integer getSuccessStatusCode(Void input, PublishingRequestCaseDto output) {
        return HTTP_OK;
    }
    
    private TicketEntry fetchPublishingRequest(RequestInfo requestInfo)
        throws NotFoundException, UnauthorizedException {
        var resourceIdentifier = extractIdentifier(requestInfo, PUBLICATION_IDENTIFIER_PATH_PARAMETER);
        var requestIdentifier = extractIdentifier(requestInfo, TICKET_IDENTIFIER_PATH_PARAMETER);
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        
        if (requestInfo.userIsAuthorized(AccessRight.APPROVE_PUBLISH_REQUEST.toString())) {
            return fetchPublishingRequestForElevatedUser(userInstance, resourceIdentifier);
        } else {
            return fetchPublishingRequestForRequestOwner(userInstance, resourceIdentifier, requestIdentifier);
        }
    }
    
    private PublishingRequestCase fetchPublishingRequestForRequestOwner(UserInstance userInstance,
                                                                        SortableIdentifier resourceIdentifier,
                                                                        SortableIdentifier requestIdentifier)
        throws NotFoundException {
        var queryObject = PublishingRequestCase.createQueryObject(userInstance,
            resourceIdentifier,
            requestIdentifier);
        return (PublishingRequestCase) ticketService.fetchTicket(queryObject);
    }
    
    private PublishingRequestCase fetchPublishingRequestForElevatedUser(UserInstance userInstance,
                                                                        SortableIdentifier resourceIdentifier) {
        return ticketService.fetchTicketByResourceIdentifier(userInstance.getOrganizationUri(),
                resourceIdentifier, PublishingRequestCase.class)
            .orElseThrow();
    }
    
    private SortableIdentifier extractIdentifier(RequestInfo requestInfo, String pathParameterName) {
        return attempt(() -> requestInfo.getPathParameter(pathParameterName)).map(SortableIdentifier::new)
            .orElseThrow();
    }
}
