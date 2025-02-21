package no.unit.nva.publication.model.business.publicationstate;

import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.FileLogEntry;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;

public record FileRejectedEvent(Instant date, User user, SortableIdentifier identifier, String rejectedFileType)
    implements FileEvent {

    public static FileRejectedEvent create(User user, Instant timestamp, String rejectedFileType) {
        return new FileRejectedEvent(timestamp, user, SortableIdentifier.next(), rejectedFileType);
    }

    @Override
    public FileLogEntry toLogEntry(FileEntry fileEntry, LogUser user) {
        return FileLogEntry.builder()
                   .withIdentifier(identifier)
                   .withFileIdentifier(fileEntry.getIdentifier())
                   .withResourceIdentifier(fileEntry.getResourceIdentifier())
                   .withTopic(LogTopic.FILE_REJECTED)
                   .withTimestamp(date)
                   .withPerformedBy(user)
                   .withFilename(fileEntry.getFile().getName())
                   .withFileType(rejectedFileType)
                   .build();
    }
}
