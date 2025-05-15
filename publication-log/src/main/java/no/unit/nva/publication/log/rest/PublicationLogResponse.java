package no.unit.nva.publication.log.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collection;
import java.util.List;
import no.unit.nva.publication.model.business.logentry.FileLogEntry;
import no.unit.nva.publication.model.business.logentry.LogEntry;
import no.unit.nva.publication.model.business.logentry.PublicationLogEntry;
import no.unit.nva.publication.model.business.logentry.TicketLogEntry;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(PublicationLogResponse.TYPE)
public record PublicationLogResponse(List<LogEntryDto> logEntries) {

    public static final String TYPE = "PublicationLog";

    public static PublicationLogResponse fromLogEntries(Collection<LogEntry> logEntries) {
        var logEntriesDto = logEntries.stream().map(PublicationLogResponse::toLogEntryDto).toList();
        return new PublicationLogResponse(logEntriesDto);
    }

    private static LogEntryDto toLogEntryDto(LogEntry logEntry) {
        return switch (logEntry) {
            case PublicationLogEntry publicationLogEntry -> PublicationLogEntryDto.fromLogEntry(publicationLogEntry);
            case FileLogEntry fileLogEntry -> FileLogEntryDto.fromLogEntry(fileLogEntry);
            case TicketLogEntry ticketLogEntry -> TicketLogEntryDto.fromLogEntry(ticketLogEntry);
            default -> throw new IllegalStateException("Unknown logentry: " + logEntry.getClass().getSimpleName());
        };
    }
}
