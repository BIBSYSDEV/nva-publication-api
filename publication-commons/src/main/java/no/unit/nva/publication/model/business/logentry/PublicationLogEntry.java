package no.unit.nva.publication.model.business.logentry;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.publication.service.impl.ResourceService;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record PublicationLogEntry(SortableIdentifier identifier, SortableIdentifier resourceIdentifier, LogTopic topic,
                                  Instant timestamp, LogAgent performedBy, ImportSource importSource)
    implements LogEntry {

    public static final String TYPE = "PublicationLogEntry";

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void persist(ResourceService resourceService) {
        resourceService.persistLogEntry(this);
    }

    public static final class Builder {

        private SortableIdentifier identifier;
        private SortableIdentifier resourceIdentifier;
        private LogTopic topic;
        private Instant timestamp;
        private LogAgent performedBy;
        private ImportSource importSource;

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

        public Builder withPerformedBy(LogAgent performedBy) {
            this.performedBy = performedBy;
            return this;
        }

        public Builder withImportSource(ImportSource importSource) {
            this.importSource = importSource;
            return this;
        }

        public PublicationLogEntry build() {
            return new PublicationLogEntry(identifier, resourceIdentifier, topic, timestamp, performedBy, importSource);
        }
    }
}
