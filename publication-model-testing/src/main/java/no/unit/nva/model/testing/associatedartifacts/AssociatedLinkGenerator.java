package no.unit.nva.model.testing.associatedartifacts;

import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import no.unit.nva.model.associatedartifacts.AssociatedLink;

public final class AssociatedLinkGenerator {

    private AssociatedLinkGenerator() {
        super();
    }


    public static AssociatedLink random() {
        return new AssociatedLink(randomUri(), randomString(), randomString());
    }
}
