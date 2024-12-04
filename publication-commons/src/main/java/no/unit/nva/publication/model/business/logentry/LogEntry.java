package no.unit.nva.publication.model.business.logentry;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.service.impl.ResourceService;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record LogEntry(SortableIdentifier identifier, SortableIdentifier publicationIdentifier, LogTopic topic,
                       Instant timestamp, User performedBy, URI institution) {

    public static final String TYPE = "LogEntry";

    public void persist(ResourceService resourceService) {
        resourceService.persistLogEntry(this);
    }
}
