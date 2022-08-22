package no.unit.nva.publication.publishingrequest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.publishingrequest.create.DoiRequestDto;
import no.unit.nva.publication.publishingrequest.create.PublishingRequestDto;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = DoiRequestDto.TYPE, value = DoiRequestDto.class),
    @JsonSubTypes.Type(name = PublishingRequestDto.TYPE, value = PublishingRequestDto.class)
})
public interface TicketDto {
    
 
    
    Class<? extends TicketEntry> ticketType();
}
