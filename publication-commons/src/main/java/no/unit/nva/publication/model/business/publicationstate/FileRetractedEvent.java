package no.unit.nva.publication.model.business.publicationstate;

import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.FileLogEntry;
import no.unit.nva.publication.model.business.logentry.LogAgent;
import no.unit.nva.publication.model.business.logentry.LogTopic;

public record FileRetractedEvent(Instant date, User user, SortableIdentifier identifier)
    implements FileEvent {

    public static FileRetractedEvent create(User user, Instant timestamp) {
        return new FileRetractedEvent(timestamp, user, SortableIdentifier.next());
    }

    @Override
    public FileLogEntry toLogEntry(FileEntry fileEntry, LogAgent user) {
        return FileLogEntry.builder()
                   .withIdentifier(identifier)
                   .withFileIdentifier(fileEntry.getIdentifier())
                   .withResourceIdentifier(fileEntry.getResourceIdentifier())
                   .withTopic(LogTopic.FILE_RETRACTED)
                   .withTimestamp(date)
                   .withPerformedBy(user)
                   .withFilename(fileEntry.getFile().getName())
                   .withFileType(fileEntry.getFile().getClass().getSimpleName())
                   .build();
    }
}
