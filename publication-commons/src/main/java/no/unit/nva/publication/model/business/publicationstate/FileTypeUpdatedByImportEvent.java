package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.FileLogEntry;
import no.unit.nva.publication.model.business.logentry.LogAgent;
import no.unit.nva.publication.model.business.logentry.LogTopic;

public record FileTypeUpdatedByImportEvent(Instant date, User user, URI institution, SortableIdentifier identifier,
                                           ImportSource importSource) implements FileEvent, ImportEvent  {

    public static FileTypeUpdatedByImportEvent create(User user, URI institution, Instant timestamp, ImportSource importSource) {
        return new FileTypeUpdatedByImportEvent(timestamp, user, institution, SortableIdentifier.next(), importSource);
    }

    @Override
    public FileLogEntry toLogEntry(FileEntry fileEntry, LogAgent user) {
        return FileLogEntry.builder()
                   .withIdentifier(identifier)
                   .withFileIdentifier(fileEntry.getIdentifier())
                   .withResourceIdentifier(fileEntry.getResourceIdentifier())
                   .withTopic(LogTopic.FILE_TYPE_UPDATED_BY_IMPORT)
                   .withTimestamp(date)
                   .withPerformedBy(user)
                   .withFilename(fileEntry.getFile().getName())
                   .withFileType(fileEntry.getFile().getClass().getSimpleName())
                   .withImportSource(importSource)
                   .build();
    }
}
