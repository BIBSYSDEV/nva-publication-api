package no.unit.nva.publication.log.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.publication.model.business.logentry.LogAgent;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.PublicationLogEntry;

@JsonTypeName(PublicationLogEntry.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record PublicationLogEntryDto(LogTopic topic, Instant timestamp, LogAgent performedBy,
                                     SortableIdentifier publicationIdentifier, ImportSource importSource)
    implements LogEntryDto {

    public static PublicationLogEntryDto fromLogEntry(PublicationLogEntry publicationLogEntry) {
        return new PublicationLogEntryDto(publicationLogEntry.topic(), publicationLogEntry.timestamp(),
                                          publicationLogEntry.performedBy(), publicationLogEntry.resourceIdentifier(),
                                          publicationLogEntry.importSource());
    }
}
