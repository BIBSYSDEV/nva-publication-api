package no.unit.nva.publication.ticket.update;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.doi.DataCiteDoiClient;
import no.unit.nva.doi.DoiClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketConfig;
import no.unit.nva.publication.ticket.TicketHandler;
import no.unit.nva.publication.ticket.model.identityservice.TicketRequest;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;

import java.net.http.HttpClient;

public class UpdateTicketHandler extends TicketHandler<TicketRequest, Void> {

    public static final String API_HOST = "API_HOST";
    private final TicketService ticketService;
    private final ResourceService resourceService;
    private final DoiClient doiClient;

    @JacocoGenerated
    public UpdateTicketHandler() {
        this(TicketService.defaultService(),
                ResourceService.defaultService(), new DataCiteDoiClient(HttpClient.newHttpClient(),
                        SecretsReader.defaultSecretsManagerClient(),
                        new Environment().readEnv(API_HOST)));
    }

    protected UpdateTicketHandler(TicketService ticketService, ResourceService resourceService,
                                  DoiClient doiClient) {
        super(TicketRequest.class);
        this.ticketService = ticketService;
        this.resourceService = resourceService;
        this.doiClient = doiClient;
    }

    @Override
    protected Void processInput(TicketRequest input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var ticket = ticketService.fetchTicketByIdentifier(extractTicketIdentifierFromPath(requestInfo));
        hasEffectiveChanges(ticket, input);

    }

    private boolean hasEffectiveChanges(TicketEntry ticket, TicketRequest ticketRequest) {
        return ticketRequest.getStatus().equals(ticket.getStatus())
                && ticketRequest.getAssignee().equals(ticket.getAssignee());
    }

    private static SortableIdentifier extractTicketIdentifierFromPath(RequestInfo requestInfo) {
        return new SortableIdentifier(requestInfo.getPathParameter(TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME));
    }

    @Override
    protected Integer getSuccessStatusCode(TicketRequest input, Void output) {
        return null;
    }
}
