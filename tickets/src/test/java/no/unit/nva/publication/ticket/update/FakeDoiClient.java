package no.unit.nva.publication.ticket.update;

import static java.util.Objects.nonNull;
import java.net.URI;
import no.unit.nva.doi.CreateFindableDoiClient;
import no.unit.nva.model.Publication;
import no.unit.nva.testutils.RandomDataGenerator;

public class FakeDoiClient implements CreateFindableDoiClient {

    @Override
    public URI createFindableDoi(Publication publication) {
        return nonNull(publication.getDoi())
                   ? publication.getDoi()
                   : RandomDataGenerator.randomDoi();
    }
}
