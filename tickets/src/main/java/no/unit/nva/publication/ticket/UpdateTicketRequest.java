package no.unit.nva.publication.ticket;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.ticket.update.ViewStatus;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(UpdateTicketRequest.TYPE)
public record UpdateTicketRequest(TicketStatus status, Username assignee, ViewStatus viewStatus)
    implements TicketRequest {

    static final String TYPE = "UpdateTicketRequest";
}
