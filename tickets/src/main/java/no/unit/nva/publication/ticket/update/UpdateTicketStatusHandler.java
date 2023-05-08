package no.unit.nva.publication.ticket.update;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import no.unit.nva.doi.DataCiteDoiClient;
import no.unit.nva.doi.DoiClient;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketHandler;
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
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateTicketStatusHandler extends TicketHandler<TicketDto, Void> {

    public static final String PUBLICATION_WITH_IDENTIFIER_S_DOES_NOT_SATISFY_DOI_REQUIREMENTS
        = "Publication with identifier  %s, does not satisfy DOI requirements";
    public static final String COULD_NOT_CREATE_FINDABLE_DOI = "Could not create findable doi";
    private static final Logger logger = LoggerFactory.getLogger(UpdateTicketStatusHandler.class);
    public static final String EXCEPTION_MESSAGE = "Creating findable doi failed with exception: {}";
    private final TicketService ticketService;
    private final ResourceService resourceService;
    private final DoiClient doiClient;
    private static final String ACCESS_RIGHT_APPROVE_PUBLISH_REQUEST = AccessRight.APPROVE_PUBLISH_REQUEST.toString();
    private static final String ACCESS_RIGHT_APPROVE_DOI_REQUEST = AccessRight.APPROVE_DOI_REQUEST.toString();

    @JacocoGenerated
    public UpdateTicketStatusHandler() {
        this(TicketService.defaultService(),
             ResourceService.defaultService(), new DataCiteDoiClient(HttpClient.newHttpClient(),
                                                                     SecretsReader.defaultSecretsManagerClient(),
                                                                     new Environment().readEnv("API_HOST")));
    }

    public UpdateTicketStatusHandler(TicketService ticketService, ResourceService resourceService,
                                     DoiClient doiClient) {
        super(TicketDto.class);
        this.ticketService = ticketService;
        this.resourceService = resourceService;
        this.doiClient = doiClient;
    }

    @Override
    protected Void processInput(TicketDto input, RequestInfo requestInfo, Context context) throws ApiGatewayException {

        var ticketIdentifier = extractTicketIdentifierFromPath(requestInfo);
        var ticket = ticketService.fetchTicketByIdentifier(ticketIdentifier);

        if (userIsNotAuthorized(requestInfo, ticket)) {
            throw new ForbiddenException();
        }
        if (ticket instanceof DoiRequest) {
            doiTicketSideEffects(input, requestInfo);
        }
        ticketService.updateTicketStatus(ticket, input.getStatus(), getUsername(requestInfo));
        return null;
    }

    private static Username getUsername(RequestInfo requestInfo) throws UnauthorizedException {
        return new Username(requestInfo.getUserName());
    }

    @Override
    protected Integer getSuccessStatusCode(TicketDto input, Void output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }

    private static boolean userIsNotAuthorized(RequestInfo requestInfo, TicketEntry ticket)
        throws UnauthorizedException {
        return !(isAuthorizedToCompleteTickets(requestInfo) && isUserFromSameCustomerAsTicket(requestInfo, ticket));
    }

    private static boolean isUserFromSameCustomerAsTicket(RequestInfo requestInfo, TicketEntry ticket)
        throws UnauthorizedException {
        return requestInfo.getCurrentCustomer().equals(ticket.getCustomerId());
    }

    private static boolean isAuthorizedToCompleteTickets(RequestInfo requestInfo) {
        return userIsAuthorizedToApproveDoiRequest(requestInfo)
                || userIsAuthorizedToApprovePublishingRequest(requestInfo);
    }

    private static boolean userIsAuthorizedToApprovePublishingRequest(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(ACCESS_RIGHT_APPROVE_PUBLISH_REQUEST);
    }

    private static boolean userIsAuthorizedToApproveDoiRequest(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(ACCESS_RIGHT_APPROVE_DOI_REQUEST);
    }

    private void doiTicketSideEffects(TicketDto input, final RequestInfo requestInfo)
        throws NotFoundException, BadMethodException, BadGatewayException {
        var status = input.getStatus();
        if (TicketStatus.COMPLETED.equals(status)) {
            findableDoiTicketSideEffects(requestInfo);
        }
        if (TicketStatus.CLOSED.equals(status) && hasDoi(getPublication(requestInfo))) {
            deleteDoiTicketSideEffects(getPublication(requestInfo));
        }
    }

    private void deleteDoiTicketSideEffects(Publication publication) {
        doiClient.deleteDraftDoi(publication);
        publication.setDoi(null);
        resourceService.updatePublication(publication);
    }

    private boolean hasDoi(Publication publication) {
        return nonNull(publication.getDoi());
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
            logger.error(EXCEPTION_MESSAGE,  e);
            throw new BadGatewayException(COULD_NOT_CREATE_FINDABLE_DOI);
        }
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

    private void publicationSatisfiesDoiRequirements(Publication publication)
        throws BadMethodException {
        if (!publication.satisfiesFindableDoiRequirements()) {
            throw new BadMethodException(
                String.format(PUBLICATION_WITH_IDENTIFIER_S_DOES_NOT_SATISFY_DOI_REQUIREMENTS,
                              publication.getIdentifier()));
        }
    }
}
