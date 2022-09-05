package no.unit.nva.publication.publishingrequest;

import java.util.List;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;

public class GeneralSupportRequestDto extends TicketDto {
    
    private TicketStatus ticketStatus;
    private List<MessageDto> messages;
    
    
    
    
    @Override
    public Class<? extends TicketEntry> ticketType() {
        return GeneralSupportRequest.class;
    }
    
    @Override
    public TicketEntry toTicket() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public TicketStatus getStatus() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public List<MessageDto> getMessages() {
        throw new UnsupportedOperationException();
    }
}
