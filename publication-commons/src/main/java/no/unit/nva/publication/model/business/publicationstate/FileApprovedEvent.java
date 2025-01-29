package no.unit.nva.publication.model.business.publicationstate;

import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.FileLogEntry;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;

public record FileApprovedEvent(Instant date, User user) implements FileEvent {

    public static FileApprovedEvent create(User user, Instant timestamp) {
        return new FileApprovedEvent(timestamp, user);
    }

    @Override
    public FileLogEntry toLogEntry(FileEntry fileEntry, LogUser user) {
        return FileLogEntry.builder()
                   .withIdentifier(SortableIdentifier.next())
                   .withFileIdentifier(fileEntry.getIdentifier())
                   .withResourceIdentifier(fileEntry.getResourceIdentifier())
                   .withTopic(LogTopic.FILE_APPROVED)
                   .withTimestamp(date)
                   .withPerformedBy(user)
                   .withFilename(fileEntry.getFile().getName())
                   .withFileType(fileEntry.getFile().getClass().getSimpleName())
                   .build();
    }
}
