package no.unit.nva.publication.log.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.LogEntry;
import no.unit.nva.publication.model.business.logentry.LogTopic;

@JsonTypeName(LogEntryDto.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record LogEntryDto(LogTopic logTopic, Instant timestamp, User performedBy, URI institution) {

    public static final String TYPE = "LogEntry";

    public static LogEntryDto fromLogEntry(LogEntry logEntry) {
        return new LogEntryDto(logEntry.topic(), logEntry.timestamp(), logEntry.performedBy(), logEntry.institution());
    }
}
