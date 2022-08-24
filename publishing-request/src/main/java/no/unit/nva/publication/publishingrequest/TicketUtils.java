package no.unit.nva.publication.publishingrequest;

import static no.unit.nva.publication.PublicationServiceConfig.defaultDynamoDbClient;
import java.time.Clock;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

//TODO: Rename or refactor class
public final class TicketUtils {
    
    public static final String TICKET_IDENTIFIER_PATH_PARAMETER = "ticketIdentifier";
    
    private TicketUtils() {
    
    }
    
    public static UserInstance createUserInstance(RequestInfo requestInfo) throws UnauthorizedException {
        return UserInstance.create(requestInfo.getNvaUsername(), requestInfo.getCurrentCustomer());
    }
    
    @JacocoGenerated
    public static TicketService defaultRequestService() {
        return new TicketService(defaultDynamoDbClient(), Clock.systemDefaultZone());
    }
}
