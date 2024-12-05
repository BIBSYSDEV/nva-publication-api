package no.unit.nva.publication.model.business.logentry;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.service.impl.ResourceService;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record LogEntry(SortableIdentifier identifier, SortableIdentifier resourceIdentifier, LogTopic topic,
                       Instant timestamp, User performedBy, URI institution) {

    public static Builder builder() {
        return new Builder();
    }

    public void persist(ResourceService resourceService) {
        resourceService.persistLogEntry(this);
    }

    public static final class Builder {

        private SortableIdentifier identifier;
        private SortableIdentifier resourceIdentifier;
        private LogTopic topic;
        private Instant timestamp;
        private User performedBy;
        private URI institution;

        private Builder() {
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withResourceIdentifier(SortableIdentifier publicationIdentifier) {
            this.resourceIdentifier = publicationIdentifier;
            return this;
        }

        public Builder withTopic(LogTopic topic) {
            this.topic = topic;
            return this;
        }

        public Builder withTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withPerformedBy(User performedBy) {
            this.performedBy = performedBy;
            return this;
        }

        public Builder withInstitution(URI institution) {
            this.institution = institution;
            return this;
        }

        public LogEntry build() {
            return new LogEntry(identifier, resourceIdentifier, topic, timestamp, performedBy, institution);
        }
    }
}
