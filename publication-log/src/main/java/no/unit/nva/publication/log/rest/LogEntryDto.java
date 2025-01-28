package no.unit.nva.publication.log.rest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.time.Instant;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(name = FileLogEntryDto.TYPE, value = FileLogEntryDto.class),
    @JsonSubTypes.Type(name = PublicationLogEntryDto.TYPE, value = PublicationLogEntryDto.class),
    })
public interface LogEntryDto {

    LogTopic topic();

    Instant timestamp();

    LogUser performedBy();

}
