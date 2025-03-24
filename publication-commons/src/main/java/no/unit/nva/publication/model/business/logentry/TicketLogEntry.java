package no.unit.nva.publication.model.business.logentry;

import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.service.impl.ResourceService;

public record TicketLogEntry(SortableIdentifier identifier, SortableIdentifier ticketIdentifier,
                             SortableIdentifier resourceIdentifier, LogTopic topic, Instant timestamp,
                             LogAgent performedBy) implements LogEntry {

    public static final String TYPE = "TicketLogEntry";

    public static Builder builder() {
        return new Builder();
    }

    public void persist(ResourceService resourceService) {
        resourceService.persistLogEntry(this);
    }

    public static final class Builder {

        private SortableIdentifier identifier;
        private SortableIdentifier ticketIdentifier;
        private SortableIdentifier resourceIdentifier;
        private LogTopic topic;
        private Instant timestamp;
        private LogAgent performedBy;

        private Builder() {
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withTicketIdentifier(SortableIdentifier ticketIdentifier) {
            this.ticketIdentifier = ticketIdentifier;
            return this;
        }

        public Builder withResourceIdentifier(SortableIdentifier resourceIdentifier) {
            this.resourceIdentifier = resourceIdentifier;
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

        public TicketLogEntry build() {
            return new TicketLogEntry(identifier, ticketIdentifier, resourceIdentifier, topic, timestamp, performedBy);
        }
    }
}
