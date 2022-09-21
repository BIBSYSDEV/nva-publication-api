package no.unit.nva.publication.publishingrequest;

import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME;
import static no.unit.nva.publication.publishingrequest.TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME;
import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.NotFoundException;

public abstract class TicketHandler<I, O> extends ApiGatewayHandler<I, O> {
    
    public static final String TICKET_NOT_FOUND = "Ticket not found";
    
    protected TicketHandler(Class<I> iclass) {
        super(iclass);
    }
    
    protected static SortableIdentifier extractTicketIdentifierFromPath(RequestInfo requestInfo)
        throws NotFoundException {
        return attempt(() -> requestInfo.getPathParameter(TICKET_IDENTIFIER_PARAMETER_NAME))
                   .map(SortableIdentifier::new)
                   .orElseThrow(fail -> new NotFoundException(TICKET_NOT_FOUND));
    }
    
    protected static SortableIdentifier extractPublicationIdentifierFromPath(RequestInfo requestInfo) {
        return new SortableIdentifier(requestInfo.getPathParameter(PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME));
    }
}
