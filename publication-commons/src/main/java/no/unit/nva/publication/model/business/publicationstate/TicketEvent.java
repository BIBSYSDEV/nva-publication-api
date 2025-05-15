package no.unit.nva.publication.model.business.publicationstate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.LogAgent;
import no.unit.nva.publication.model.business.logentry.TicketLogEntry;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(DoiRequestedEvent.class), @JsonSubTypes.Type(DoiAssignedEvent.class),
    @JsonSubTypes.Type(DoiRejectedEvent.class)})
public interface TicketEvent {

    Instant date();

    User user();

    SortableIdentifier identifier();

    /**
     * @return id of the top level cristin organizations
     */
    URI institution();

    TicketLogEntry toLogEntry(SortableIdentifier resourceIdentifier, SortableIdentifier ticketIdentifier, LogAgent user);
}
