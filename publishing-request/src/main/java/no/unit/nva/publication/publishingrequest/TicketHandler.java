package no.unit.nva.publication.publishingrequest;

import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;

public abstract class TicketHandler<I, O> extends ApiGatewayHandler<I, O> {
    
    protected TicketHandler(Class<I> iclass) {
        super(iclass);
    }
    
    protected static SortableIdentifier extractPublicationIdentifierFromPath(RequestInfo requestInfo) {
        var identifierString = requestInfo.getPathParameter(PUBLICATION_IDENTIFIER_PATH_PARAMETER);
        return new SortableIdentifier(identifierString);
    }
    
    protected static SortableIdentifier extractTicketIdentifierFromPath(RequestInfo requestInfo) {
        return new SortableIdentifier(requestInfo.getPathParameter(TicketUtils.TICKET_IDENTIFIER_PATH_PARAMETER));
    }
}
