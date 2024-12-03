package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;

public record CreatedState(Instant date, User user, URI institution) implements State {

    public static CreatedState create(UserInstance userInstance, Instant date) {
        return new CreatedState(date, userInstance.getUser(), userInstance.getTopLevelOrgCristinId());
    }
}
