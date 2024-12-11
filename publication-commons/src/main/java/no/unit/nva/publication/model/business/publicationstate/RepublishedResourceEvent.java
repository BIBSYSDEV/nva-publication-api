package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.LogEntry;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;

public record RepublishedResourceEvent(Instant date, User user, URI institution) implements ResourceEvent {

    public static RepublishedResourceEvent create(UserInstance userInstance, Instant date) {
        return new RepublishedResourceEvent(date, userInstance.getUser(), userInstance.getTopLevelOrgCristinId());
    }

    @Override
    public LogEntry toLogEntry(SortableIdentifier resourceIdentifier, LogUser user) {
        return LogEntry.builder()
                   .withResourceIdentifier(resourceIdentifier)
                   .withIdentifier(SortableIdentifier.next())
                   .withTopic(LogTopic.PUBLICATION_REPUBLISHED)
                   .withTimestamp(Instant.now())
                   .withPerformedBy(user)
                   .build();
    }
}
