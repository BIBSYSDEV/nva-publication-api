package no.unit.nva.publication.ticket.update;

import java.net.URI;
import no.unit.nva.doi.DoiClient;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.Resource;

public class FakeDoiClientThrowingException implements DoiClient {

    @Override
    public URI generateDraftDoi(URI requestingCustomer) {
        throw new RuntimeException("I dont work");
    }

    @Override
    public URI createFindableDoi(URI requestingCustomer, Publication publication) {
        throw new RuntimeException("I dont work");
    }

    @Override
    public void deleteDraftDoi(URI requestingCustomer, Publication publication) {
        throw new RuntimeException("I dont work");
    }
}
