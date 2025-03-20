package no.unit.nva.publication.log.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.logentry.LogAgent;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.TicketLogEntry;

@JsonTypeName(TicketLogEntry.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record TicketLogEntryDto(LogTopic topic, Instant timestamp, LogAgent performedBy,
                                SortableIdentifier publicationIdentifier, SortableIdentifier ticketIdentifier)
    implements LogEntryDto {

    public static TicketLogEntryDto fromLogEntry(TicketLogEntry logEntry) {
        return new TicketLogEntryDto(logEntry.topic(), logEntry.timestamp(), logEntry.performedBy(),
                                     logEntry.resourceIdentifier(), logEntry.ticketIdentifier());
    }
}
