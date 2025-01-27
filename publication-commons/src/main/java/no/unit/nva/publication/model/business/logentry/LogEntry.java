package no.unit.nva.publication.model.business.logentry;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(names = {PublicationLogEntry.TYPE, "LogEntry"}, value = PublicationLogEntry.class),
    @JsonSubTypes.Type(name = FileLogEntry.TYPE, value = FileLogEntry.class)})
public interface LogEntry {

    SortableIdentifier identifier();

    SortableIdentifier resourceIdentifier();

    LogTopic topic();

    Instant timestamp();

    LogUser performedBy();
}
