package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.PublicationLogEntry;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;

public record CreatedResourceEvent(Instant date, User user, URI institution) implements ResourceEvent {

    public static CreatedResourceEvent create(UserInstance userInstance, Instant date) {
        return new CreatedResourceEvent(date, userInstance.getUser(), userInstance.getTopLevelOrgCristinId());
    }

    @Override
    public PublicationLogEntry toLogEntry(SortableIdentifier resourceIdentifier, LogUser user) {
        return PublicationLogEntry.builder()
                   .withResourceIdentifier(resourceIdentifier)
                   .withIdentifier(SortableIdentifier.next())
                   .withTopic(LogTopic.PUBLICATION_CREATED)
                   .withTimestamp(Instant.now())
                   .withPerformedBy(user)
                   .build();
    }
}
