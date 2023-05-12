package no.unit.nva.publication.ticket;

import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;

import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME;

public abstract class TicketHandler<I, O> extends ApiGatewayHandler<I, O> {
    
    public static final String TICKET_NOT_FOUND = "Ticket not found";
    
    protected TicketHandler(Class<I> iclass) {
        super(iclass);
    }
    
    protected static SortableIdentifier extractPublicationIdentifierFromPath(RequestInfo requestInfo) {
        return new SortableIdentifier(requestInfo.getPathParameter(PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME));
    }
}
