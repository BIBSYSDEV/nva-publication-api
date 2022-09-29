package no.unit.nva.doirequest.update;

import static no.unit.nva.doirequest.DoiRequestRelatedAccessRights.APPROVE_DOI_REQUEST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Collections;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.exception.NotImplementedException;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.PublicationDetails;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

public class UpdateDoiRequestStatusHandler extends ApiGatewayHandler<ApiUpdateDoiRequest, Void> {
    
    public static final String INVALID_PUBLICATION_ID_ERROR = "Invalid publication id: ";
    
    public static final String API_HOST_ENV_VARIABLE = "API_HOST";
    public static final String API_SCHEME = "https";
    private static final String LOCATION_TEMPLATE_PUBLICATION = "%s://%s/publication/%s";
    private final String apiHost;
    private final TicketService ticketService;
    
    @JacocoGenerated
    public UpdateDoiRequestStatusHandler() {
        this(TicketService.defaultService());
    }
    
    public UpdateDoiRequestStatusHandler(TicketService ticketService) {
        super(ApiUpdateDoiRequest.class);
        this.apiHost = environment.readEnv(API_HOST_ENV_VARIABLE);
        this.ticketService = ticketService;
    }
    
    @Override
    protected Void processInput(ApiUpdateDoiRequest input,
                                RequestInfo requestInfo,
                                Context context)
        throws ApiGatewayException {
        
        try {
            input.validate();
            SortableIdentifier publicationIdentifier = getPublicationIdentifier(requestInfo);
            UserInstance userInstance = createUserInstance(requestInfo);
            validateUser(requestInfo);
            updateDoiRequestStatus(userInstance, input.getDoiRequestStatus(), publicationIdentifier);
            updateContentLocationHeader(publicationIdentifier);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new BadRequestException(e.getMessage());
        }
        return null;
    }
    
    @Override
    protected Integer getSuccessStatusCode(ApiUpdateDoiRequest input, Void output) {
        return HttpStatus.SC_ACCEPTED;
    }
    
    private void validateUser(RequestInfo requestInfo) throws NotAuthorizedException {
        if (userIsNotAuthorized(requestInfo)) {
            throw new NotAuthorizedException();
        }
    }
    
    private boolean userIsNotAuthorized(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(APPROVE_DOI_REQUEST.toString());
    }
    
    private UserInstance createUserInstance(RequestInfo requestInfo) throws UnauthorizedException {
        String user = requestInfo.getNvaUsername();
        var customerId = requestInfo.getCurrentCustomer();
        return UserInstance.create(user, customerId);
    }
    
    private void updateDoiRequestStatus(UserInstance userInstance,
                                        TicketStatus newTicketStatus,
                                        SortableIdentifier publicationIdentifier)
        throws ApiGatewayException {
        TicketEntry ticketEntry = DoiRequest.builder()
                                      .withCustomerId(userInstance.getOrganizationUri())
                                      .withPublicationDetails(PublicationDetails.create(publicationIdentifier))
            .withStatus(newTicketStatus)
            .build();
        if (TicketStatus.COMPLETED.equals(newTicketStatus)) {
            ticketService.updateTicketStatus(ticketEntry, TicketStatus.COMPLETED);
        } else {
            //TODO: implement rejection in the service
            throw new NotImplementedException();
        }
    }
    
    private void updateContentLocationHeader(SortableIdentifier publicationIdentifier) {
        addAdditionalHeaders(
            () -> Collections.singletonMap(HttpHeaders.LOCATION, getContentLocation(publicationIdentifier)));
    }
    
    private String getContentLocation(SortableIdentifier publicationID) {
        return String.format(LOCATION_TEMPLATE_PUBLICATION, API_SCHEME, apiHost, publicationID.toString());
    }
    
    private SortableIdentifier getPublicationIdentifier(RequestInfo requestInfo) throws BadRequestException {
        String publicationIdentifierString = requestInfo.getPathParameter(PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME);
        return attempt(() -> new SortableIdentifier(publicationIdentifierString))
            .orElseThrow(
                fail -> new BadRequestException(INVALID_PUBLICATION_ID_ERROR + publicationIdentifierString));
    }
}
