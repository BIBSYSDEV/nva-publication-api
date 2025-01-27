package no.unit.nva.publication.log.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collection;
import java.util.List;
import no.unit.nva.publication.model.business.logentry.LogEntry;
import no.unit.nva.publication.model.business.logentry.PublicationLogEntry;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(PublicationLogResponse.TYPE)
public record PublicationLogResponse(List<LogEntryDto> logEntries) {

    public static final String TYPE = "PublicationLog";

    public static PublicationLogResponse fromLogEntries(Collection<LogEntry> logEntries) {
        var logEntriesDto = logEntries.stream().map(LogEntryDto::fromLogEntry).toList();
        return new PublicationLogResponse(logEntriesDto);
    }
}
