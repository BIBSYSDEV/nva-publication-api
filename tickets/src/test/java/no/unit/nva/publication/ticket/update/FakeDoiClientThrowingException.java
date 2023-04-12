package no.unit.nva.publication.ticket.update;

import java.net.URI;
import no.unit.nva.doi.DoiClient;
import no.unit.nva.model.Publication;

public class FakeDoiClientThrowingException implements DoiClient {

    @Override
    public URI generateDraftDoi(Publication publication) {
        throw new RuntimeException("I dont work");
    }

    @Override
    public URI createFindableDoi(Publication publication) {
        throw new RuntimeException("I dont work");
    }

    @Override
    public void deleteDraftDoi(Publication publication) {
        throw new RuntimeException("I dont work");
    }
}
