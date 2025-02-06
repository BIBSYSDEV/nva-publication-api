package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;
import no.unit.nva.publication.model.business.logentry.PublicationLogEntry;

public record CreatedResourceEvent(Instant date, User user, URI institution, SortableIdentifier identifier)
    implements ResourceEvent {

    public static CreatedResourceEvent create(UserInstance userInstance, Instant date) {
        return new CreatedResourceEvent(date, userInstance.getUser(), userInstance.getTopLevelOrgCristinId(),
                                        SortableIdentifier.next());
    }

    @Override
    public PublicationLogEntry toLogEntry(SortableIdentifier resourceIdentifier, LogUser user) {
        return PublicationLogEntry.builder()
                   .withResourceIdentifier(resourceIdentifier)
                   .withIdentifier(identifier)
                   .withTopic(LogTopic.PUBLICATION_CREATED)
                   .withTimestamp(date)
                   .withPerformedBy(user)
                   .build();
    }
}
