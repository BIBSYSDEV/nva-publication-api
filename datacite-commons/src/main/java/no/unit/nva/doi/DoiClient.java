package no.unit.nva.doi;

import java.net.URI;
import no.unit.nva.model.Publication;

public interface DoiClient {

    URI generateDraftDoi(URI requestingCustomer);

    URI createFindableDoi(URI requestingCustomer, Publication publication);

    void deleteDraftDoi(URI requestingCustomer, Publication publication);

}
