package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;
import no.unit.nva.publication.model.business.logentry.PublicationLogEntry;

public record DeletedResourceEvent(Instant date, User user, URI institution, SortableIdentifier identifier)
    implements ResourceEvent {

    public static DeletedResourceEvent create(UserInstance userInstance, Instant date) {
        return new DeletedResourceEvent(date, userInstance.getUser(), userInstance.getTopLevelOrgCristinId(),
                                        SortableIdentifier.next());
    }

    @Override
    public PublicationLogEntry toLogEntry(SortableIdentifier resourceIdentifier, LogUser user) {
        return PublicationLogEntry.builder()
                   .withResourceIdentifier(resourceIdentifier)
                   .withIdentifier(identifier)
                   .withTopic(LogTopic.PUBLICATION_DELETED)
                   .withTimestamp(date)
                   .withPerformedBy(user)
                   .build();
    }
}
