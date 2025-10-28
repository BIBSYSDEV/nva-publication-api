package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.ImportSource.Source;
import no.unit.nva.publication.model.business.ThirdPartySystem;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.LogAgent;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.PublicationLogEntry;
import nva.commons.core.JacocoGenerated;

public record CreatedResourceEvent(Instant date, User user, URI institution, SortableIdentifier identifier,
                                   ImportSource importSource)
    implements ResourceEvent {

    public static CreatedResourceEvent create(UserInstance userInstance, Instant date) {
        return new CreatedResourceEvent(date, userInstance.getUser(), userInstance.getTopLevelOrgCristinId(),
                                        SortableIdentifier.next(), userInstance.isExternalClient()
                                                                       ? getImportSource(userInstance)
                                                                       : null);
    }

    private static ImportSource getImportSource(UserInstance userInstance) {
        return userInstance.getThirdPartySystem()
                   .map(CreatedResourceEvent::toSource)
                   .map(ImportSource::fromSource)
                   .orElse(ImportSource.fromSource(Source.OTHER));

    }

    @JacocoGenerated
    private static Source toSource(ThirdPartySystem thirdPartySystem) {
        return switch (thirdPartySystem) {
            case INSPERA -> Source.INSPERA;
            case WISE_FLOW -> Source.WISE_FLOW;
            case OTHER -> Source.OTHER;
        };
    }

    @Override
    public PublicationLogEntry toLogEntry(SortableIdentifier resourceIdentifier, LogAgent user) {
        return PublicationLogEntry.builder()
                   .withResourceIdentifier(resourceIdentifier)
                   .withIdentifier(identifier)
                   .withTopic(LogTopic.PUBLICATION_CREATED)
                   .withTimestamp(date)
                   .withImportSource(importSource)
                   .withPerformedBy(user)
                   .build();
    }
}
