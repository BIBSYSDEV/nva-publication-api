package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;

public record DeletedResourceEvent(Instant date, User user, URI institution) implements ResourceEvent {

    public static DeletedResourceEvent create(UserInstance userInstance, Instant date) {
        return new DeletedResourceEvent(date, userInstance.getUser(), userInstance.getTopLevelOrgCristinId());
    }
}
