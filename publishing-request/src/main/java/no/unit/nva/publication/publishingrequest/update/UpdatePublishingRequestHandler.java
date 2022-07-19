package no.unit.nva.publication.publishingrequest.update;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.publishingrequest.PublishingRequestCaseDto;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public class UpdatePublishingRequestHandler
    extends ApiGatewayHandler<PublishingRequestCaseDto, PublishingRequestCaseDto> {
    
    public static final String AUTHORIZATION_ERROR = "User is not authorized to approve publishing requests";
    private final PublishingRequestService requestService;
    
    public UpdatePublishingRequestHandler(PublishingRequestService requestService) {
        super(PublishingRequestCaseDto.class);
        this.requestService = requestService;
    }
    
    @Override
    protected PublishingRequestCaseDto processInput(PublishingRequestCaseDto input,
                                                    RequestInfo requestInfo,
                                                    Context context) throws ApiGatewayException {
        authorizeUser(requestInfo);
        var publicationIdentifier =
            readIdentifierFromPathParameter(requestInfo, PUBLICATION_IDENTIFIER_PATH_PARAMETER);
        var publishingRequestIdentifier =
            readIdentifierFromPathParameter(requestInfo, PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER);
        
        validateInput(input, publicationIdentifier, publishingRequestIdentifier);
        var currentRequest =
            requestService.getPublishingRequestByPublicationAndRequestIdentifiers(publicationIdentifier,
                publishingRequestIdentifier);
        
        final var updatedRequest = currentRequest.approve();
        var updatedEntry = requestService.updatePublishingRequest(updatedRequest);
        return PublishingRequestCaseDto.createResponseObject(updatedEntry);
    }
    
    @Override
    protected Integer getSuccessStatusCode(PublishingRequestCaseDto input, PublishingRequestCaseDto output) {
        return HTTP_OK;
    }
    
    private void validateInput(PublishingRequestCaseDto input,
                               SortableIdentifier publicationIdentifier,
                               SortableIdentifier publishingRequestIdentifier) throws BadRequestException {
        var expectedId = PublishingRequestCaseDto.calculateId(publicationIdentifier, publishingRequestIdentifier);
        if (doNotMatch(input.getId(), expectedId)) {
            throw new BadRequestException("Id in path parameter does not match id in body");
        }
    }
    
    private boolean doNotMatch(URI left, URI right) {
        return !left.equals(right);
    }
    
    private void authorizeUser(RequestInfo requestInfo) throws UnauthorizedException {
        if (!requestInfo.userIsAuthorized(AccessRight.APPROVE_PUBLISH_REQUEST.toString())) {
            throw new UnauthorizedException(AUTHORIZATION_ERROR);
        }
    }
    
    private SortableIdentifier readIdentifierFromPathParameter(RequestInfo requestInfo,
                                                               String pathParameter) {
        return attempt(() -> requestInfo.getPathParameter(pathParameter))
            .map(SortableIdentifier::new)
            .orElseThrow();
    }
}
