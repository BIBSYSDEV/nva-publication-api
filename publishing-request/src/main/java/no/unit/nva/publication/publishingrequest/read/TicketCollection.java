package no.unit.nva.publication.publishingrequest.read;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Collections;
import java.util.List;
import no.unit.nva.publication.publishingrequest.TicketDto;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class TicketCollection {
    
    public static final String TICKETS_FIELD = "tickets";
    
    @JsonProperty(TICKETS_FIELD)
    private final List<TicketDto> tickets;
    
    public TicketCollection(@JsonProperty(TICKETS_FIELD) List<TicketDto> tickets) {
        this.tickets = tickets;
    }
    
    public static TicketCollection fromTickets(List<TicketDto> tickets) {
        return new TicketCollection(tickets);
    }
    
    public List<TicketDto> getTickets() {
        return nonNull(tickets) ? tickets : Collections.emptyList();
    }
}
