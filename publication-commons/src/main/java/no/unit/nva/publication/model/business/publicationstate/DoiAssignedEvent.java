package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;
import no.unit.nva.publication.model.business.logentry.TicketLogEntry;

public record DoiAssignedEvent(Instant date, User user, URI institution, SortableIdentifier identifier)
    implements TicketEvent {

    public static DoiAssignedEvent create(UserInstance userInstance, Instant date) {
        return new DoiAssignedEvent(date, userInstance.getUser(), userInstance.getTopLevelOrgCristinId(),
                                     SortableIdentifier.next());
    }

    @Override
    public TicketLogEntry toLogEntry(SortableIdentifier resourceIdentifier,
                                     SortableIdentifier ticketIdentifier, LogUser user) {
        return TicketLogEntry.builder()
                   .withTicketIdentifier(ticketIdentifier)
                   .withResourceIdentifier(resourceIdentifier)
                   .withIdentifier(identifier)
                   .withTopic(LogTopic.DOI_ASSIGNED)
                   .withTimestamp(date)
                   .withPerformedBy(user)
                   .build();
    }
}
