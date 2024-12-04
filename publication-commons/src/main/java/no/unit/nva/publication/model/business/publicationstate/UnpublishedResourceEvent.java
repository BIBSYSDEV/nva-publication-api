package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.LogEntry;
import no.unit.nva.publication.model.business.logentry.LogTopic;

public record UnpublishedResourceEvent(Instant date, User user, URI institution) implements ResourceEvent {

    public static UnpublishedResourceEvent create(UserInstance userInstance, Instant date) {
        return new UnpublishedResourceEvent(date, userInstance.getUser(), userInstance.getTopLevelOrgCristinId());
    }

    @Override
    public LogEntry toLogEntry(SortableIdentifier resourceIdentifier) {
        return LogEntry.builder()
                   .withResourceIdentifier(resourceIdentifier)
                   .withIdentifier(SortableIdentifier.next())
                   .withTopic(LogTopic.PUBLICATION_UNPUBLISHED)
                   .withTimestamp(Instant.now())
                   .withPerformedBy(user())
                   .withInstitution(institution())
                   .build();
    }
}
