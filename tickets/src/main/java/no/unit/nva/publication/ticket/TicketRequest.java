package no.unit.nva.publication.ticket;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = UpdateTicketRequest.class)
@JsonSubTypes({@JsonSubTypes.Type(name = UpdateTicketRequest.TYPE, value = UpdateTicketRequest.class),
    @JsonSubTypes.Type(name = UpdateTicketOwnershipRequest.TYPE, value = UpdateTicketOwnershipRequest.class),})
public interface TicketRequest {

}
