package no.unit.nva.publication.log.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import no.unit.nva.publication.model.business.logentry.LogEntry;
import no.unit.nva.publication.model.business.logentry.PublicationLogEntry;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;

@JsonTypeName(LogEntryDto.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record LogEntryDto(LogTopic topic, Instant timestamp, LogUser performedBy) {

    public static final String TYPE = "LogEntry";

    public static LogEntryDto fromLogEntry(LogEntry logEntry) {
        return new LogEntryDto(logEntry.topic(), logEntry.timestamp(), logEntry.performedBy());
    }
}
