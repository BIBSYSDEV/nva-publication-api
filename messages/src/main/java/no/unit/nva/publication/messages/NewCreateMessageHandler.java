package no.unit.nva.publication.messages;

import static no.unit.nva.publication.messages.MessageApiConfig.LOCATION_HEADER;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.messages.model.NewMessageDto;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class NewCreateMessageHandler extends ApiGatewayHandler<CreateMessageRequest, Void> {
    
    private final MessageService messageService;
    private final TicketService ticketService;
    
    public NewCreateMessageHandler(MessageService messageService, TicketService ticketService) {
        super(CreateMessageRequest.class);
        this.messageService = messageService;
        this.ticketService = ticketService;
    }
    
    @Override
    protected Void processInput(CreateMessageRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var ticketIdentifier = extractTicketIdentifier(requestInfo);
        var user = UserInstance.fromRequestInfo(requestInfo);
        var ticket = ticketService.fetchTicketByIdentifier(ticketIdentifier);
        var message = messageService.createMessage(ticket, user, input.getMessage());
        addAdditionalHeaders(() -> Map.of(LOCATION_HEADER, createLocationHeader(message)));
        return null;
    }
    
    @Override
    protected Integer getSuccessStatusCode(CreateMessageRequest input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }
    
    private static String createLocationHeader(Message message) {
        return NewMessageDto.constructMessageId(message).toString();
    }
    
    private static SortableIdentifier extractTicketIdentifier(RequestInfo requestInfo) {
        var identifierString = requestInfo.getPathParameter(MessageApiConfig.TICKET_IDENTIFIER_PATH_PARAMETER);
        return new SortableIdentifier(identifierString);
    }
}
