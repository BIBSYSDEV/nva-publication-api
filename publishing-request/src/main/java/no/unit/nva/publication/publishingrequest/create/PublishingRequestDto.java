package no.unit.nva.publication.publishingrequest.create;

import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.publishingrequest.TicketDto;

public class PublishingRequestDto implements TicketDto {
    
    public static final String TYPE = "PublishingRequest";
    
    @Override
    public Class<PublishingRequestCase> ticketType() {
        return PublishingRequestCase.class;
    }
}
