package no.unit.nva.publication.model.business.publicationstate;

import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.FileLogEntry;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;

public record FileDeletedEvent(Instant date, User user) implements FileEvent {

    public static FileDeletedEvent create(User user, Instant timestamp) {
        return new FileDeletedEvent(timestamp, user);
    }

    @Override
    public FileLogEntry toLogEntry(FileEntry fileEntry, LogUser user) {
        return FileLogEntry.builder()
                   .withIdentifier(SortableIdentifier.next())
                   .withFileIdentifier(fileEntry.getIdentifier())
                   .withResourceIdentifier(fileEntry.getResourceIdentifier())
                   .withTopic(LogTopic.FILE_DELETED)
                   .withTimestamp(date)
                   .withPerformedBy(user)
                   .withFilename(fileEntry.getFile().getName())
                   .build();
    }
}
