package no.unit.nva.publication;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import no.unit.nva.model.Publication;

public class TestDataSource {

    public Publication publicationWithIdentifier() {
        return randomPublication();
    }

    public Publication publicationWithoutIdentifier() {
        var publication = randomPublication();
        publication.setIdentifier(null);
        return publication;
    }
}
