package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;

public record UnpublishedResourceEvent(Instant date, User user, URI institution) implements ResourceEvent {

    public static UnpublishedResourceEvent create(UserInstance userInstance, Instant date) {
        return new UnpublishedResourceEvent(date, userInstance.getUser(), userInstance.getTopLevelOrgCristinId());
    }
}
