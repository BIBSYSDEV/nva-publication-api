package no.unit.nva.publication.publishingrequest.update;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.publishingrequest.PublishingRequestCaseDto;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.storage.model.PublishingRequestCase;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public class UpdatePublishingRequestHandler
    extends ApiGatewayHandler<PublishingRequestApproval, PublishingRequestCaseDto> {

    public static final String AUTHORIZATION_ERROR = "User is not authorized to approve publishing requests";
    private final PublishingRequestService requestService;

    public UpdatePublishingRequestHandler(PublishingRequestService requestService) {
        super(PublishingRequestApproval.class);
        this.requestService = requestService;
    }

    @Override
    protected PublishingRequestCaseDto processInput(PublishingRequestApproval input,
                                                    RequestInfo requestInfo,
                                                    Context context) throws ApiGatewayException {
        authorizeUser(requestInfo);
        var publicationIdentifier =
            readIdentifierFromPathParameter(requestInfo, PUBLICATION_IDENTIFIER_PATH_PARAMETER);
        var publishingRequestIdentifier =
            readIdentifierFromPathParameter(requestInfo, PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER);

        var publishingRequestUpdate =
            createPublishingRequestUpdate(requestInfo, publicationIdentifier, publishingRequestIdentifier);
        var currentRequest =
            requestService.getPublishingRequestByPublicationAndRequestIdentifiers(publicationIdentifier,publishingRequestIdentifier);

        var updatedRequest = currentRequest.approve();
        requestService.updatePublishingRequest(updatedRequest);
        return PublishingRequestCaseDto.createResponseObject(publishingRequestUpdate);
    }

    private void authorizeUser(RequestInfo requestInfo) throws UnauthorizedException {
        if(!requestInfo.userIsAuthorized(AccessRight.APPROVE_PUBLISH_REQUEST.toString())){
            throw new UnauthorizedException(AUTHORIZATION_ERROR);
        }
    }

    private PublishingRequestCase createPublishingRequestUpdate(RequestInfo requestInfo,
                                                                SortableIdentifier publicationIdentifier,
                                                                SortableIdentifier publishingRequestIdentifier)
        throws UnauthorizedException {
        var userInstance = UserInstance.create(requestInfo.getNvaUsername(), requestInfo.getCurrentCustomer());
        return PublishingRequestCase.createStatusUpdate(userInstance,
                                                        publicationIdentifier,
                                                        publishingRequestIdentifier,
                                                        PublishingRequestStatus.APPROVED);
    }

    private SortableIdentifier readIdentifierFromPathParameter(RequestInfo requestInfo,
                                                               String pathParameter) {
        return attempt(() -> requestInfo.getPathParameter(pathParameter))
            .map(SortableIdentifier::new)
            .orElseThrow();
    }

    @Override
    protected Integer getSuccessStatusCode(PublishingRequestApproval input, PublishingRequestCaseDto output) {
        return HTTP_OK;
    }
}
