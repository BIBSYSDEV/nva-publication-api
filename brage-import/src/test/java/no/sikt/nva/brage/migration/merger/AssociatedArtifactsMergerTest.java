package no.sikt.nva.brage.migration.merger;

import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static no.unit.nva.model.associatedartifacts.file.PublisherVersion.ACCEPTED_VERSION;
import static no.unit.nva.model.associatedartifacts.file.PublisherVersion.PUBLISHED_VERSION;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.*;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssociatedArtifactsMergerTest {

    @Test
    void shouldImportAllIncomingFilesWhenExistingAssociatedArtifactsAreEmpty() {
        var existing = AssociatedArtifactList.empty();
        var incoming = new AssociatedArtifactList(List.of(randomAssociatedLink(), randomInternalFile(), randomHiddenFile(),
                randomOpenFile(null), randomOpenFile(ACCEPTED_VERSION), randomOpenFile(PUBLISHED_VERSION)));

        var result = AssociatedArtifactsMerger.merge(existing, incoming);

        assertTrue(result.containsAll(incoming));
    }

    @Test
    void shouldKeepAllExistingFilesWhenMergingWithNonEmptyIncomingAssociatedArtifacts() {
        var existing = new AssociatedArtifactList(List.of(randomAssociatedLink(), randomInternalFile(), randomHiddenFile(),
                randomOpenFile(null), randomOpenFile(ACCEPTED_VERSION), randomOpenFile(PUBLISHED_VERSION)));
        var incoming = new AssociatedArtifactList(List.of(randomOpenFile(ACCEPTED_VERSION),
                randomOpenFile(null),
                randomOpenFile(PUBLISHED_VERSION)));

        var result = AssociatedArtifactsMerger.merge(existing, incoming);

        assertTrue(result.containsAll(existing));
    }

    @Test
    void shouldImportAllNonOpenFilesWhenMergingIncomingAssociatedArtifactsWithExisting() {
        var existing = new AssociatedArtifactList(List.of(randomOpenFile(ACCEPTED_VERSION), randomOpenFile(PUBLISHED_VERSION),
                randomHiddenFile()));
        var incoming = new AssociatedArtifactList(List.of(randomHiddenFile(), randomInternalFile()));

        var result = AssociatedArtifactsMerger.merge(existing, incoming);

        assertTrue(result.containsAll(existing));
        assertTrue(result.containsAll(incoming));
    }

    @Test
    void shouldImportAllOpenFilesWithNonPublishedVersionWhenMergingIncomingAssociatedArtifactsWithExisting() {
        var existing = new AssociatedArtifactList(List.of(randomOpenFile(ACCEPTED_VERSION), randomOpenFile(PUBLISHED_VERSION),
                randomHiddenFile()));
        var incoming = new AssociatedArtifactList(List.of(randomOpenFile(null), randomOpenFile(ACCEPTED_VERSION)));

        var result = AssociatedArtifactsMerger.merge(existing, incoming);

        assertTrue(result.containsAll(existing));
        assertTrue(result.containsAll(incoming));
    }

    @Test
    void shouldNotImportOpenFileWithPublishedVersionAndDifferentNameThanExistingOpenFileWithPublishedVersionWhenOnlyOneFile() {
        var existingOpenFile = randomOpenFile(PUBLISHED_VERSION);
        var existing = new AssociatedArtifactList(List.of(existingOpenFile));
        var incomingOpenFile = randomOpenFile(PUBLISHED_VERSION);
        var incoming = new AssociatedArtifactList(List.of(incomingOpenFile));

        var result = AssociatedArtifactsMerger.merge(existing, incoming);

        assertTrue(result.containsAll(existing));
        assertFalse(result.containsAll(incoming));
    }

    @Test
    void shouldImportOpenFileWithPublishedVersionAndDifferentNameThanExistingOpenFileWithPublishedVersionWhenMultipleFiles() {
        var existing = new AssociatedArtifactList(List.of(randomOpenFile(PUBLISHED_VERSION), randomOpenFile(PUBLISHED_VERSION)));
        var incoming = new AssociatedArtifactList(List.of(randomOpenFile(PUBLISHED_VERSION)));

        var result = AssociatedArtifactsMerger.merge(existing, incoming);

        assertTrue(result.containsAll(existing));
        assertTrue(result.containsAll(incoming));
    }

    @Test
    void shouldNotImportOpenFileWithPublishedVersionAndExistingNameThanExistingOpenFileWithPublishedVersionWhenMultipleFiles() {
        var filename = randomString();
        var existing = new AssociatedArtifactList(List.of(randomOpenFile(PUBLISHED_VERSION, filename), randomOpenFile(PUBLISHED_VERSION)));
        var incoming = new AssociatedArtifactList(List.of(randomOpenFile(PUBLISHED_VERSION, filename)));

        var result = AssociatedArtifactsMerger.merge(existing, incoming);

        assertTrue(result.containsAll(existing));
        assertFalse(result.containsAll(incoming));
    }

    @Test
    void shouldImportOpenFileWithPublishedVersionWhenThereExistsOpenFileWithoutPublishedVersionButTheSameFilename() {
        var filename = randomString();
        var existing = new AssociatedArtifactList(List.of(randomOpenFile(ACCEPTED_VERSION, filename), randomOpenFile(null, filename)));
        var incoming = new AssociatedArtifactList(List.of(randomOpenFile(PUBLISHED_VERSION, filename)));

        var result = AssociatedArtifactsMerger.merge(existing, incoming);

        assertTrue(result.containsAll(existing));
        assertTrue(result.containsAll(incoming));
    }
}