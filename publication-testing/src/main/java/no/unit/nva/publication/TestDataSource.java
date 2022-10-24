package no.unit.nva.publication;

import no.unit.nva.model.Publication;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.core.attempt.Try.attempt;

public class TestDataSource {

    public Publication publicationWithIdentifier() {
        return randomPublication();
    }

    public Publication publicationWithoutIdentifier() {
        var publication = randomPublication();
        publication.setIdentifier(null);
        return publication;
    }

    public static Publication.Builder randomPreFilledPublicationBuilder() {
        return attempt(() -> randomPublication().copy()).orElseThrow();
    }
}
