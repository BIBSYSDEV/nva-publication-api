package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;
import no.unit.nva.publication.model.business.logentry.PublicationLogEntry;

public record ImportedResourceEvent(Instant date, User user, URI institution, ImportSource importSource)
    implements ResourceEvent {

    public static ImportedResourceEvent fromImportSource(ImportSource importSource, Instant date) {
        return new ImportedResourceEvent(date, null, null, importSource);
    }

    @Override
    public PublicationLogEntry toLogEntry(SortableIdentifier resourceIdentifier, LogUser user) {
        return PublicationLogEntry.builder()
                   .withResourceIdentifier(resourceIdentifier)
                   .withIdentifier(SortableIdentifier.next())
                   .withTopic(LogTopic.PUBLICATION_IMPORTED)
                   .withTimestamp(date)
                   .withPerformedBy(user)
                   .withImportSource(importSource)
                   .build();
    }
}
