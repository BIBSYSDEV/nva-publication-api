package no.unit.nva.publication.log.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.publication.model.business.logentry.FileLogEntry;
import no.unit.nva.publication.model.business.logentry.LogAgent;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;

@JsonTypeName(FileLogEntryDto.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record FileLogEntryDto(LogTopic topic, Instant timestamp, LogAgent performedBy,
                              SortableIdentifier publicationIdentifier, SortableIdentifier fileIdentifier,
                              String filename, String fileType, ImportSource importSource) implements LogEntryDto {

    public static final String TYPE = "FileLogEntry";

    public static FileLogEntryDto fromLogEntry(FileLogEntry fileLogEntry) {
        return new FileLogEntryDto(fileLogEntry.topic(), fileLogEntry.timestamp(), fileLogEntry.performedBy(),
                                   fileLogEntry.resourceIdentifier(), fileLogEntry.fileIdentifier(),
                                   fileLogEntry.filename(), fileLogEntry.fileType(), fileLogEntry.importSource());
    }
}
