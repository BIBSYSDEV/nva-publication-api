package no.unit.nva.publication.model.business.publicationstate;

import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.FileLogEntry;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;

public record FileImportedEvent(Instant date, User user, SortableIdentifier identifier, ImportSource importSource)
    implements FileEvent {

    public static FileImportedEvent create(User user, Instant timestamp, ImportSource importSource) {
        return new FileImportedEvent(timestamp, user, SortableIdentifier.next(), importSource);
    }

    @Override
    public FileLogEntry toLogEntry(FileEntry fileEntry, LogUser user) {
        return FileLogEntry.builder()
                   .withIdentifier(identifier)
                   .withFileIdentifier(fileEntry.getIdentifier())
                   .withResourceIdentifier(fileEntry.getResourceIdentifier())
                   .withTopic(LogTopic.FILE_IMPORTED)
                   .withTimestamp(date)
                   .withPerformedBy(user)
                   .withFilename(fileEntry.getFile().getName())
                   .withFileType(fileEntry.getFile().getClass().getSimpleName())
                   .withImportSource(importSource)
                   .build();
    }
}
