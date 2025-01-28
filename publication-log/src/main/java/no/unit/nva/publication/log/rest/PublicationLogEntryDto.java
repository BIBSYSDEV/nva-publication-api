package no.unit.nva.publication.log.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;
import no.unit.nva.publication.model.business.logentry.PublicationLogEntry;

@JsonTypeName(PublicationLogEntryDto.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record PublicationLogEntryDto(LogTopic topic, Instant timestamp, LogUser performedBy) implements LogEntryDto {

    public static final String TYPE = "PublicationLogEntry";

    public static PublicationLogEntryDto fromLogEntry(PublicationLogEntry publicationLogEntry) {
        return new PublicationLogEntryDto(publicationLogEntry.topic(), publicationLogEntry.timestamp(), publicationLogEntry.performedBy());
    }
}
