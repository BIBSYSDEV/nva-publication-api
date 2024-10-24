package no.unit.nva.model.testing.associatedartifacts;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.util.List;
import java.util.UUID;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;

public final class AssociatedArtifactsGenerator {

    private AssociatedArtifactsGenerator() {
        // NO-OP
    }

    public static List<AssociatedArtifact> randomAssociatedArtifacts() {
        return new AssociatedArtifactList(PublishedFileGenerator.random(),randomAssociatedLink());
    }

    public static AssociatedLink randomAssociatedLink() {
        return new AssociatedLink(randomUri(), randomString(), randomString());
    }

    public static File randomUnpublishedFile() {
        return UnpublishedFile.builder().withIdentifier(UUID.randomUUID())
                   .withLicense(randomUri())
                   .withName(randomString())
                   .buildUnpublishedFile();
    }
}
