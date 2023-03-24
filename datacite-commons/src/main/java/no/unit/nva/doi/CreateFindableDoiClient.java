package no.unit.nva.doi;

import java.net.URI;
import no.unit.nva.model.Publication;

public interface CreateFindableDoiClient {

    URI createFindableDoi(Publication publication);

}
