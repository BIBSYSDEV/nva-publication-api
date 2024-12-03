package no.unit.nva.publication.model.business.logentry;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import no.unit.nva.publication.model.business.User;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record LogEntry(LogTopic topic, Instant timestamp, User performedBy) {

}
