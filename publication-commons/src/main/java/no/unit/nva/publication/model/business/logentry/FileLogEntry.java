package no.unit.nva.publication.model.business.logentry;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.publication.service.impl.ResourceService;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record FileLogEntry(SortableIdentifier identifier, SortableIdentifier fileIdentifier,
                           SortableIdentifier resourceIdentifier, LogTopic topic, String filename,
                           String fileType, Instant timestamp,
                           LogAgent performedBy, ImportSource importSource) implements LogEntry {

    public static final String TYPE = "FileLogEntry";

    public void persist(ResourceService resourceService) {
        resourceService.persistLogEntry(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private SortableIdentifier identifier;
        private SortableIdentifier fileIdentifier;
        private SortableIdentifier resourceIdentifier;
        private LogTopic topic;
        private String filename;
        private String fileType;
        private Instant timestamp;
        private LogAgent performedBy;
        private ImportSource importSource;

        private Builder() {
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withFileIdentifier(SortableIdentifier fileIdentifier) {
            this.fileIdentifier = fileIdentifier;
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

        public Builder withFilename(String filename) {
            this.filename = filename;
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

        public Builder withFileType(String fileType) {
            this.fileType = fileType;
            return this;
        }

        public Builder withImportSource(ImportSource importSource) {
            this.importSource = importSource;
            return this;
        }

        public FileLogEntry build() {
            return new FileLogEntry(identifier, fileIdentifier, resourceIdentifier, topic, filename, fileType,
                                    timestamp, performedBy, importSource);
        }
    }
}
