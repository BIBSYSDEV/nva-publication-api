package no.unit.nva.model.testing.associatedartifacts;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.util.List;
import java.util.UUID;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.PendingInternalFile;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
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

    public static File randomPublishedFile() {
        return File.builder().withIdentifier(UUID.randomUUID())
                   .withLicense(randomUri())
                   .withName(randomString())
                   .buildPublishedFile();
    }

    public static File randomPendingOpenFile() {
        return PendingOpenFile.builder().withIdentifier(UUID.randomUUID())
                   .withLicense(randomUri())
                   .withName(randomString())
                   .buildPendingOpenFile();
    }

    public static File randomPendingInternalFile() {
        return PendingInternalFile.builder().withIdentifier(UUID.randomUUID())
                   .withLicense(randomUri())
                   .withName(randomString())
                   .buildPendingInternalFile();
    }

    public static File randomInternalFile() {
        return InternalFile.builder().withIdentifier(UUID.randomUUID())
                   .withLicense(randomUri())
                   .withName(randomString())
                   .buildInternalFile();
    }
}
