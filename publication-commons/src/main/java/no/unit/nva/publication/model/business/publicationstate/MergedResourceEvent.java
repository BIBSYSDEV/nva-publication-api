package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;
import no.unit.nva.publication.model.business.logentry.PublicationLogEntry;

public record MergedResourceEvent(Instant date, User user, URI institution, ImportSource importSource,
                                  SortableIdentifier identifier) implements ResourceEvent {

    public static MergedResourceEvent fromImportSource(ImportSource importSource, UserInstance userInstance,
                                                         Instant date) {
        return new MergedResourceEvent(date, userInstance.getUser(), userInstance.getTopLevelOrgCristinId(),
                                         importSource, SortableIdentifier.next());
    }

    @Override
    public PublicationLogEntry toLogEntry(SortableIdentifier resourceIdentifier, LogUser user) {
        return PublicationLogEntry.builder()
                   .withResourceIdentifier(resourceIdentifier)
                   .withIdentifier(identifier)
                   .withTopic(LogTopic.PUBLICATION_MERGED)
                   .withTimestamp(date)
                   .withPerformedBy(user)
                   .withImportSource(importSource)
                   .build();
    }
}
