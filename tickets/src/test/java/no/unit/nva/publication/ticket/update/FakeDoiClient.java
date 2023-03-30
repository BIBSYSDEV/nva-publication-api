package no.unit.nva.publication.ticket.update;

import static java.util.Objects.nonNull;
import java.net.URI;
import no.unit.nva.doi.DoiClient;
import no.unit.nva.model.Publication;
import no.unit.nva.testutils.RandomDataGenerator;

public class FakeDoiClient implements DoiClient {

    @Override
    public URI generateDraftDoi(Publication publication) {
        throw new IllegalArgumentException("Method is not used yet");
    }

    @Override
    public URI createFindableDoi(Publication publication) {
        return nonNull(publication.getDoi())
                   ? publication.getDoi()
                   : RandomDataGenerator.randomDoi();
    }

    @Override
    public void deleteDraftDoi(Publication publication) {}
}
