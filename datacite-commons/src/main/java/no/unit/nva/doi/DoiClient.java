package no.unit.nva.doi;

import java.net.URI;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.Resource;

public interface DoiClient {

    URI generateDraftDoi(Resource publication);

    URI createFindableDoi(Publication publication);

    void deleteDraftDoi(Publication publication);

}
