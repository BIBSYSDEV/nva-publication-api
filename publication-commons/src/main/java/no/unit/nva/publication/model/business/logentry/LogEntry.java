package no.unit.nva.publication.model.business.logentry;

import java.time.Instant;
import no.unit.nva.publication.model.business.User;

public record LogEntry(LogTopic topic, Instant timestamp, User handledBy) {

}
