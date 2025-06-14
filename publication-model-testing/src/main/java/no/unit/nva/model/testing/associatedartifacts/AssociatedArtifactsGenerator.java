package no.unit.nva.model.testing.associatedartifacts;

import static no.unit.nva.model.associatedartifacts.file.File.FINALIZED_FILE_TYPES;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RelationType;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.File.Builder;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;

public final class AssociatedArtifactsGenerator {

    public static final NullRightsRetentionStrategy NULL_RIGHTS_RETENTION_STRATEGY = NullRightsRetentionStrategy.create(
        RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY);

    private AssociatedArtifactsGenerator() {
        // NO-OP
    }

    public static List<AssociatedArtifact> randomAssociatedArtifacts() {
        return new AssociatedArtifactList(randomPendingOpenFile(), randomOpenFile(), randomPendingInternalFile(),
                                          randomInternalFile(), randomAssociatedLink(), randomHiddenFile());
    }

    public static List<AssociatedArtifact> randomAssociatedArtifactsIncludingUploadedFileAndRejectedFile() {
        var artifacts = randomAssociatedArtifacts();
        artifacts.add(randomUploadedFile());
        artifacts.add(randomRejectedFile());
        return artifacts;
    }

    public static List<AssociatedArtifact> randomNonFinalizedFiles() {
        return randomAssociatedArtifactsIncludingUploadedFileAndRejectedFile()
                   .stream()
                   .filter(File.class::isInstance)
                   .filter(artifact -> !FINALIZED_FILE_TYPES.contains(artifact.getClass()))
                   .toList();
    }

    public static List<AssociatedArtifact> randomFinalizedFiles() {
        return randomAssociatedArtifactsIncludingUploadedFileAndRejectedFile()
                   .stream()
                   .filter(File.class::isInstance)
                   .filter(artifact -> FINALIZED_FILE_TYPES.contains(artifact.getClass()))
                   .toList();
    }

    public static AssociatedLink randomAssociatedLink() {
        return new AssociatedLink(randomUri(), randomString(), randomString(), RelationType.SAME_AS);
    }

    public static File randomPendingOpenFile() {
        return randomFileBuilder().buildPendingOpenFile();
    }

    public static File randomUploadedFile() {
        return randomFileBuilder().buildUploadedFile();
    }

    public static File randomRejectedFile() {
        return randomFileBuilder().buildRejectedFile();
    }

    public static File randomOpenFile() {
        return randomFileBuilder().buildOpenFile();
    }

    public static File randomOpenFile(PublisherVersion version) {
        return randomFileBuilder()
                   .withPublisherVersion(version)
                   .buildOpenFile();
    }

    public static File randomOpenFile(PublisherVersion version, String name) {
        return randomFileBuilder()
                   .withPublisherVersion(version)
                   .withName(name)
                   .buildOpenFile();
    }

    public static File randomPendingInternalFile() {
        return randomFileBuilder().buildPendingInternalFile();
    }

    public static File randomInternalFile() {
        return randomFileBuilder().buildInternalFile();
    }

    public static File randomHiddenFile() {
        return randomFileBuilder().buildHiddenFile();
    }

    public static File randomOpenFileWithLicense(URI license) {
        return randomFileBuilder().withLicense(license).buildOpenFile();
    }

    private static Builder randomFileBuilder() {
        return File.builder()
                   .withIdentifier(UUID.randomUUID())
                   .withLicense(randomUri())
                   .withName(randomString())
                   .withLicense(randomUri())
                   .withEmbargoDate(Instant.now())
                   .withLegalNote(randomString())
                   .withRightsRetentionStrategy(NULL_RIGHTS_RETENTION_STRATEGY)
                   .withMimeType(randomString())
                   .withSize(Integer.toUnsignedLong(randomInteger()))
                   .withPublisherVersion(PublisherVersion.PUBLISHED_VERSION)
                   .withUploadDetails(new UserUploadDetails(new Username(randomString()), Instant.now()));
    }
}
