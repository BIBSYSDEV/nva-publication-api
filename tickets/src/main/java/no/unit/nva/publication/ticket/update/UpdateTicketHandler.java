package no.unit.nva.publication.ticket.update;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.publication.ticket.TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.net.http.HttpClient;
import no.unit.nva.doi.DataCiteDoiClient;
import no.unit.nva.doi.DoiClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketHandler;
import no.unit.nva.publication.ticket.UpdateTicketRequest;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadMethodException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.GodClass")
public class UpdateTicketHandler extends TicketHandler<UpdateTicketRequest, Void> {

    public static final String API_HOST = "API_HOST";
    public static final String EXCEPTION_MESSAGE = "Creating findable doi failed with exception: {}";
    public static final String COULD_NOT_CREATE_FINDABLE_DOI = "Could not create findable doi";
    public static final String PUBLICATION_WITH_IDENTIFIER_S_DOES_NOT_SATISFY_DOI_REQUIREMENTS = "Publication with "
                                                                                                 + "identifier  %s, "
                                                                                                 + "does not satisfy "
                                                                                                 + "DOI requirements";
    public static final String UNKNOWN_VIEWED_STATUS_MESSAGE = "Unknown ViewedStatus";
    private static final Logger logger = LoggerFactory.getLogger(UpdateTicketHandler.class);
    private static final String ACCESS_RIGHT_APPROVE_PUBLISH_REQUEST = AccessRight.APPROVE_PUBLISH_REQUEST.toString();
    private static final String ACCESS_RIGHT_APPROVE_DOI_REQUEST = AccessRight.APPROVE_DOI_REQUEST.toString();
    private final TicketService ticketService;
    private final ResourceService resourceService;
    private final DoiClient doiClient;

    @JacocoGenerated
    public UpdateTicketHandler() {
        this(TicketService.defaultService(), ResourceService.defaultService(),
             new DataCiteDoiClient(HttpClient.newHttpClient(), SecretsReader.defaultSecretsManagerClient(),
                                   new Environment().readEnv(API_HOST)));
    }

    protected UpdateTicketHandler(TicketService ticketService, ResourceService resourceService, DoiClient doiClient) {
        super(UpdateTicketRequest.class);
        this.ticketService = ticketService;
        this.resourceService = resourceService;
        this.doiClient = doiClient;
    }

    protected static SortableIdentifier extractTicketIdentifierFromPath(RequestInfo requestInfo)
        throws NotFoundException {
        return attempt(() -> requestInfo.getPathParameter(TICKET_IDENTIFIER_PARAMETER_NAME)).map(
            SortableIdentifier::new).orElseThrow(fail -> new NotFoundException(TICKET_NOT_FOUND));
    }

    @Override
    protected Void processInput(UpdateTicketRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var ticketIdentifier = extractTicketIdentifierFromPath(requestInfo);
        var ticket = fetchTicketForElevatedUser(ticketIdentifier, UserInstance.fromRequestInfo(requestInfo));
        if (hasEffectiveChanges(ticket, input)) {
            updateTicket(input, requestInfo, ticket);
        }
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(UpdateTicketRequest input, Void output) {
        return HTTP_ACCEPTED;
    }

    private static void throwExceptionIfUnauthorized(RequestInfo requestInfo, TicketEntry ticket)
        throws UnauthorizedException, ForbiddenException {
        if (userIsNotAuthorized(requestInfo, ticket)) {
            throw new ForbiddenException();
        }
    }

    private static boolean statusHasBeenUpdated(TicketEntry ticket, UpdateTicketRequest ticketRequest) {
        return nonNull(ticketRequest.getStatus()) && !ticketRequest.getStatus().equals(ticket.getStatus());
    }

    private static boolean assigneeHasBeenUpdated(TicketEntry ticket, UpdateTicketRequest ticketRequest) {
        return incomingAssigneeIsPresent(ticketRequest) && (assigneeDoesNotExist(ticket) || assigneesAreDifferent(
            ticket, ticketRequest));
    }

    private static boolean assigneesAreDifferent(TicketEntry ticket, UpdateTicketRequest ticketRequest) {
        return !ticketRequest.getAssignee().equals(ticket.getAssignee());
    }

    private static boolean userIsNotAuthorized(RequestInfo requestInfo, TicketEntry ticket)
        throws UnauthorizedException {
        return !(isAuthorizedToCompleteTickets(requestInfo) && isUserFromSameCustomerAsTicket(requestInfo, ticket));
    }

    private static boolean isAuthorizedToCompleteTickets(RequestInfo requestInfo) {
        return userIsAuthorizedToApproveDoiRequest(requestInfo) || userIsAuthorizedToApprovePublishingRequest(
            requestInfo);
    }

    private static boolean userIsAuthorizedToApprovePublishingRequest(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(ACCESS_RIGHT_APPROVE_PUBLISH_REQUEST);
    }

    private static boolean userIsAuthorizedToApproveDoiRequest(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(ACCESS_RIGHT_APPROVE_DOI_REQUEST);
    }

    private static boolean isUserFromSameCustomerAsTicket(RequestInfo requestInfo, TicketEntry ticket)
        throws UnauthorizedException {
        return requestInfo.getCurrentCustomer().equals(ticket.getCustomerId());
    }

    private static Username getUsername(RequestInfo requestInfo) throws UnauthorizedException {
        return new Username(requestInfo.getUserName());
    }

    private static boolean assigneeDoesNotExist(TicketEntry ticket) {
        return existingAssigneeIsEmpty(ticket);
    }

    private static boolean assigneeIsEmptyString(UpdateTicketRequest ticketRequest) {
        return StringUtils.EMPTY_STRING.equals(ticketRequest.getAssignee().getValue());
    }

    private static boolean existingAssigneeIsEmpty(TicketEntry ticket) {
        return isNull(ticket.getAssignee());
    }

    private static boolean incomingAssigneeIsPresent(UpdateTicketRequest ticketRequest) {
        return nonNull(ticketRequest.getAssignee());
    }

    private static boolean userIsTicketAssignee(TicketEntry ticketEntry, String userName) {
        var assignee = ticketEntry.getAssignee();
        return nonNull(assignee) && assignee.toString().equals(userName);
    }

    private void updateTicket(UpdateTicketRequest ticketRequest, RequestInfo requestInfo, TicketEntry ticket)
        throws ApiGatewayException {
        if (incomingUpdateIsStatus(ticket, ticketRequest)) {
            updateStatus(ticketRequest, requestInfo, ticket);
        }
        if (incomingUpdateIsAssignee(ticket, ticketRequest)) {
            updateAssignee(ticketRequest, requestInfo, ticket);
        }
        if (incomingUpdateIsViewedStatus(ticketRequest)) {
            updateTicketViewedBy(ticketRequest, ticket, requestInfo);
        }
    }

    private void updateAssignee(UpdateTicketRequest ticketRequest, RequestInfo requestInfo, TicketEntry ticket)
        throws ApiGatewayException {
        throwExceptionIfUnauthorized(requestInfo, ticket);
        if (assigneeIsEmptyString(ticketRequest)) {
            ticketService.updateTicketAssignee(ticket, null);
        } else {
            ticketService.updateTicketAssignee(ticket, ticketRequest.getAssignee());
        }
    }

    private void updateStatus(UpdateTicketRequest ticketRequest, RequestInfo requestInfo, TicketEntry ticket)
        throws ApiGatewayException {
        throwExceptionIfUnauthorized(requestInfo, ticket);
        if (ticket instanceof DoiRequest) {
            doiTicketSideEffects(ticketRequest, requestInfo);
        }
        ticketService.updateTicketStatus(ticket, ticketRequest.getStatus(), getUsername(requestInfo));
    }

    private TicketEntry fetchTicketForElevatedUser(SortableIdentifier ticketIdentifier, UserInstance userInstance)
        throws ForbiddenException {
        return attempt(() -> ticketService.fetchTicketForElevatedUser(userInstance, ticketIdentifier))
                   .orElseThrow(fail -> new ForbiddenException());
    }

    private void updateTicketViewedBy(UpdateTicketRequest ticketRequest, TicketEntry ticket, RequestInfo requestInfo)
        throws ApiGatewayException {
        assertThatPublicationIdentifierInPathReferencesCorrectPublication(ticket, requestInfo);
        if (userIsTicketAssignee(ticket, requestInfo.getUserName())) {
            throwExceptionIfUnauthorized(requestInfo, ticket);
            markTicketForAssignee(ticketRequest, ticket);
        } else if (userIsTicketOwner(ticket, requestInfo.getUserName())) {
            markTicketForOwner(ticketRequest, ticket);
        }
    }

    private boolean userIsTicketOwner(TicketEntry ticket, String userName) {
        var owner = ticket.getOwner();
        return nonNull(owner) && owner.toString().equals(userName);
    }

    private void markTicketForOwner(UpdateTicketRequest input, TicketEntry ticket) {
        if (ViewStatus.READ.equals(input.getViewStatus())) {
            ticket.markReadByOwner().persistUpdate(ticketService);
        } else if (ViewStatus.UNREAD.equals(input.getViewStatus())) {
            ticket.markUnreadByOwner().persistUpdate(ticketService);
        } else {
            throw new UnsupportedOperationException(UNKNOWN_VIEWED_STATUS_MESSAGE);
        }
    }

    private void markTicketForAssignee(UpdateTicketRequest input, TicketEntry ticket) {

        if (ViewStatus.READ.equals(input.getViewStatus())) {
            ticket.markReadForAssignee().persistUpdate(ticketService);
        } else if (ViewStatus.UNREAD.equals(input.getViewStatus())) {
            ticket.markUnReadForAssignee().persistUpdate(ticketService);
        } else {
            throw new UnsupportedOperationException(UNKNOWN_VIEWED_STATUS_MESSAGE);
        }
    }

    private void assertThatPublicationIdentifierInPathReferencesCorrectPublication(TicketEntry ticket,
                                                                                   RequestInfo requestInfo)
        throws ForbiddenException {
        var suppliedPublicationIdentifier = extractPublicationIdentifierFromPath(requestInfo);
        if (!suppliedPublicationIdentifier.equals(ticket.getResourceIdentifier())) {
            throw new ForbiddenException();
        }
    }

    private boolean incomingUpdateIsViewedStatus(UpdateTicketRequest input) {
        return nonNull(input.getViewStatus());
    }

    private boolean incomingUpdateIsAssignee(TicketEntry ticket, UpdateTicketRequest ticketRequest) {
        return nonNull(ticketRequest.getAssignee()) && !ticketRequest.getAssignee().equals(ticket.getAssignee());
    }

    private boolean incomingUpdateIsStatus(TicketEntry ticket, UpdateTicketRequest ticketRequest) {
        return nonNull(ticketRequest.getStatus()) && !ticket.getStatus().equals(ticketRequest.getStatus());
    }

    private boolean hasEffectiveChanges(TicketEntry ticket, UpdateTicketRequest ticketRequest) {
        return statusHasBeenUpdated(ticket, ticketRequest)
               || assigneeHasBeenUpdated(ticket, ticketRequest)
               || viewStatusHasBeenUpdated(ticketRequest);
    }

    private boolean viewStatusHasBeenUpdated(UpdateTicketRequest ticketRequest) {
        return nonNull(ticketRequest.getViewStatus());
    }

    private void doiTicketSideEffects(UpdateTicketRequest input, final RequestInfo requestInfo)
        throws NotFoundException, BadMethodException, BadGatewayException {
        var status = input.getStatus();
        if (TicketStatus.COMPLETED.equals(status)) {
            findableDoiTicketSideEffects(requestInfo);
        }
        if (TicketStatus.CLOSED.equals(status) && hasDoi(getPublication(requestInfo))) {
            deleteDoiTicketSideEffects(getPublication(requestInfo));
        }
    }

    private void findableDoiTicketSideEffects(RequestInfo requestInfo)
        throws NotFoundException, BadMethodException, BadGatewayException {
        var publication = getPublication(requestInfo);
        publicationSatisfiesDoiRequirements(publication);
        createFindableDoiAndPersistDoiOnPublication(publication);
    }

    private void createFindableDoiAndPersistDoiOnPublication(Publication publication) throws BadGatewayException {
        try {
            var doi = doiClient.createFindableDoi(publication);
            updatePublication(publication, doi);
        } catch (Exception e) {
            logger.error(EXCEPTION_MESSAGE, e.getMessage());
            throw new BadGatewayException(COULD_NOT_CREATE_FINDABLE_DOI);
        }
    }

    private boolean hasDoi(Publication publication) {
        return nonNull(publication.getDoi());
    }

    private void deleteDoiTicketSideEffects(Publication publication) {
        doiClient.deleteDraftDoi(publication);
        publication.setDoi(null);
        resourceService.updatePublication(publication);
    }

    private void updatePublication(Publication publication, URI doi) {
        if (isNull(publication.getDoi())) {
            publication.setDoi(doi);
            resourceService.updatePublication(publication);
        }
    }

    private Publication getPublication(RequestInfo requestInfo) throws NotFoundException {
        var publicationIdentifier = extractPublicationIdentifierFromPath(requestInfo);
        return resourceService.getPublicationByIdentifier(publicationIdentifier);
    }

    private void publicationSatisfiesDoiRequirements(Publication publication) throws BadMethodException {
        if (!publication.satisfiesFindableDoiRequirements()) {
            throw new BadMethodException(String.format(PUBLICATION_WITH_IDENTIFIER_S_DOES_NOT_SATISFY_DOI_REQUIREMENTS,
                                                       publication.getIdentifier()));
        }
    }
}
