package no.unit.nva.publication.log.rest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.time.Instant;
import no.unit.nva.publication.model.business.logentry.FileLogEntry;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;
import no.unit.nva.publication.model.business.logentry.PublicationLogEntry;
import no.unit.nva.publication.model.business.logentry.TicketLogEntry;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(name = FileLogEntry.TYPE, value = FileLogEntryDto.class),
    @JsonSubTypes.Type(name = PublicationLogEntry.TYPE, value = PublicationLogEntryDto.class),
    @JsonSubTypes.Type(name = TicketLogEntry.TYPE, value = TicketLogEntryDto.class)
    })
public interface LogEntryDto {

    LogTopic topic();

    Instant timestamp();

    LogUser performedBy();

}
