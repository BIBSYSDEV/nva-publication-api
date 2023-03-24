package no.unit.nva.publication.ticket.update;

import java.net.URI;
import no.unit.nva.doi.CreateFindableDoiClient;
import no.unit.nva.model.Publication;

public class FakeDoiClientThrowingException implements CreateFindableDoiClient {

    @Override
    public URI createFindableDoi(Publication publication) {
        throw new RuntimeException("I dont work");
    }
}
