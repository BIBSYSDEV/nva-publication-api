package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.LogAgent;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.PublicationLogEntry;

public record RepublishedResourceEvent(Instant date, User user, URI institution, SortableIdentifier identifier)
    implements ResourceEvent {

    public static RepublishedResourceEvent create(UserInstance userInstance, Instant date) {
        return new RepublishedResourceEvent(date, userInstance.getUser(), userInstance.getTopLevelOrgCristinId(),
                                            SortableIdentifier.next());
    }

    @Override
    public PublicationLogEntry toLogEntry(SortableIdentifier resourceIdentifier, LogAgent user) {
        return PublicationLogEntry.builder()
                   .withResourceIdentifier(resourceIdentifier)
                   .withIdentifier(identifier)
                   .withTopic(LogTopic.PUBLICATION_REPUBLISHED)
                   .withTimestamp(date)
                   .withPerformedBy(user)
                   .build();
    }
}
