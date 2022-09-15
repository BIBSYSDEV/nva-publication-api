package no.unit.nva.publication.publishingrequest;

import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;

public abstract class TicketHandler<I, O> extends ApiGatewayHandler<I, O> {
    
    protected TicketHandler(Class<I> iclass) {
        super(iclass);
    }
    
    protected static SortableIdentifier extractTicketIdentifierFromPath(RequestInfo requestInfo) {
        return new SortableIdentifier(requestInfo.getPathParameter(TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME));
    }
}
