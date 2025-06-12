package no.sikt.nva.brage.migration.merger;

import static no.unit.nva.model.associatedartifacts.file.PublisherVersion.ACCEPTED_VERSION;
import static no.unit.nva.model.associatedartifacts.file.PublisherVersion.PUBLISHED_VERSION;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomAssociatedLink;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomHiddenFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomInternalFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import org.junit.jupiter.api.Test;

class AssociatedArtifactsMergerTest {

    @Test
    void shouldKeepAllNonOpenFilesAndLinksWhenMergingAssociatedArtifacts() {
        var existing = new AssociatedArtifactList(
            List.of(randomAssociatedLink(), randomInternalFile(), randomHiddenFile()));
        var incoming = new AssociatedArtifactList(
            List.of(randomAssociatedLink(), randomInternalFile(), randomHiddenFile()));

        var result = AssociatedArtifactsMerger.merge(existing, incoming);

        assertTrue(result.containsAll(existing));
        assertTrue(result.containsAll(incoming));
    }

    @Test
    void shouldKeepAllExistingFilesAndAddOpenFilesWhenExistingDoesNotContainOpenFiles() {
        var existing = new AssociatedArtifactList(
            List.of(randomAssociatedLink(), randomInternalFile(), randomHiddenFile()));
        var incoming = new AssociatedArtifactList(List.of(randomOpenFile()));

        var result = AssociatedArtifactsMerger.merge(existing, incoming);

        assertTrue(result.containsAll(existing));
        assertTrue(result.containsAll(incoming));
    }

    @Test
    void shouldConvertExistingOpenFileWithAcceptedVersionToInternalFileWhenIncomingFilesContainOpenFileWithPublishedVersion() {
        var existing = new AssociatedArtifactList(List.of(randomOpenFile(ACCEPTED_VERSION)));
        var incoming = new AssociatedArtifactList(List.of(randomOpenFile(PUBLISHED_VERSION)));

        var result = AssociatedArtifactsMerger.merge(existing, incoming);

        assertFalse(result.containsAll(existing));
    }

    @Test
    void shouldConvertExistingOpenFileWithoutPublishedVersionToInternalWhenIncomingFilesContainOpenFileWithPublishedVersion() {
        var openFile = randomOpenFile(null);
        var existing = new AssociatedArtifactList(List.of(openFile));
        var incoming = new AssociatedArtifactList(List.of(randomOpenFile(PUBLISHED_VERSION)));

        var result = AssociatedArtifactsMerger.merge(existing, incoming);

        assertTrue(result.stream().anyMatch(InternalFile.class::isInstance));
        assertTrue(result.containsAll(incoming));
    }

    @Test
    void shouldKeepIncomingOpenFileWithPublishedVersionWhenThereIsNoExistingOpenFileWithPublishedVersionWithTheSameFileName() {
        var existing = new AssociatedArtifactList(List.of(randomOpenFile(PUBLISHED_VERSION)));
        var incoming = new AssociatedArtifactList(List.of(randomOpenFile(PUBLISHED_VERSION)));

        var result = AssociatedArtifactsMerger.merge(existing, incoming);

        assertTrue(result.containsAll(existing));
        assertTrue(result.containsAll(incoming));
    }

    @Test
    void shouldKeepExistingOpenFileWithPublishedVersionWhenIncomingOpenFileWithPublishedVersionHasTheSameFileName() {

        var filename = randomString();
        var existing = new AssociatedArtifactList(List.of(randomOpenFile(PUBLISHED_VERSION, filename)));
        var incoming = new AssociatedArtifactList(List.of(randomOpenFile(PUBLISHED_VERSION, filename)));

        var result = AssociatedArtifactsMerger.merge(existing, incoming);

        assertTrue(result.containsAll(existing));
    }

    @Test
    void shouldNotKeepIncomingOpenFileWithPublishedVersionWhenThereExistsOpenFileWithPublishedVersionWithTheSameFileName() {
        var filename = randomString();
        var existing = new AssociatedArtifactList(List.of(randomOpenFile(PUBLISHED_VERSION, filename)));
        var incoming = new AssociatedArtifactList(List.of(randomOpenFile(PUBLISHED_VERSION, filename)));

        var result = AssociatedArtifactsMerger.merge(existing, incoming);

        assertFalse(result.containsAll(incoming));
    }
}