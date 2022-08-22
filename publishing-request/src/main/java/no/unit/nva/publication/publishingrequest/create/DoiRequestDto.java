package no.unit.nva.publication.publishingrequest.create;

import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.publishingrequest.TicketDto;

public class DoiRequestDto implements TicketDto {
    
    public static final String TYPE = "DoiRequest";
    
    
    @Override
    public  Class<DoiRequest> ticketType() {
        return DoiRequest.class;
    }
}
