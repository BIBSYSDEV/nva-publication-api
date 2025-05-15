package no.unit.nva.publication.model.business.publicationstate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.FileLogEntry;
import no.unit.nva.publication.model.business.logentry.LogAgent;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(FileUploadedEvent.class), @JsonSubTypes.Type(FileApprovedEvent.class),
    @JsonSubTypes.Type(FileRejectedEvent.class), @JsonSubTypes.Type(FileDeletedEvent.class),
    @JsonSubTypes.Type(FileImportedEvent.class), @JsonSubTypes.Type(FileRetractedEvent.class),
    @JsonSubTypes.Type(FileHiddenEvent.class), @JsonSubTypes.Type(FileTypeUpdatedEvent.class)})
public interface FileEvent {

    Instant date();

    User user();

    SortableIdentifier identifier();

    FileLogEntry toLogEntry(FileEntry fileEntry, LogAgent user);
}
