package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.publication.model.business.User;

public record CreatedState(Instant date, User user, URI institution) implements State {

}
