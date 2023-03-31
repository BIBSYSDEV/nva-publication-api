package no.unit.nva.doi;

import java.net.URI;
import no.unit.nva.model.Publication;

public interface DoiClient {

    URI generateDraftDoi(Publication publication);
    URI createFindableDoi(Publication publication);
    void deleteDraftDoi(Publication publication);

}
