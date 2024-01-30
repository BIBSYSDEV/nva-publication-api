package no.unit.nva.publication;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import java.net.URI;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;

@SuppressWarnings({"PMD.TestClassWithoutTestCases"})
public class TestDataSource {

    public Publication publicationWithIdentifier() {
        return randomPublication();
    }

    public Publication publicationWithoutIdentifier() {
        var publication = randomPublication();
        publication.setIdentifier(null);
        return publication;
    }

    public Publication publicationWithoutIdentifier(URI publisherId) {
        var publication = randomPublication()
                              .copy()
                              .withPublisher(new Organization.Builder()
                                                 .withId(publisherId)
                                                 .build())
                              .build();
        publication.setIdentifier(null);
        return publication;
    }
}
