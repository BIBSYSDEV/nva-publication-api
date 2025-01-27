package no.unit.nva.publication.model.business.publicationstate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.FileLogEntry;
import no.unit.nva.publication.model.business.logentry.LogUser;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(FileUploadedEvent.class), @JsonSubTypes.Type(FileApprovedEvent.class),
    @JsonSubTypes.Type(FileRejectedEvent.class)})
public interface FileEvent {

    Instant date();

    User user();

    /**
     * @return id of the top level cristin organizations
     */
    URI institution();

    FileLogEntry toLogEntry(FileEntry fileEntry, LogUser user);
}
