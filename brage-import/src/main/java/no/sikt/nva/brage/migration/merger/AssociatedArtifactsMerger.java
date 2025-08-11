package no.sikt.nva.brage.migration.merger;

import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.OpenFile;

import java.util.ArrayList;
import java.util.Collection;

import static no.unit.nva.model.associatedartifacts.file.PublisherVersion.PUBLISHED_VERSION;

public final class AssociatedArtifactsMerger {

    private static final int SINGLETON = 1;

    private AssociatedArtifactsMerger() {
    }

    public static AssociatedArtifactList merge(AssociatedArtifactList existing, AssociatedArtifactList incoming) {
        var merged = new ArrayList<AssociatedArtifact>();
        merged.addAll(existing);
        merged.addAll(filterOutOpenFilesWithPublishedVersion(incoming));

        if (hasMultipleOrNoOpenFilesWithPublishedVersion(existing) || hasMultipleOrNoOpenFilesWithPublishedVersion(incoming)) {
            incoming.stream()
                    .filter(File.class::isInstance)
                    .map(File.class::cast)
                    .filter(file -> isUniqueOpenFileWithPublishedVersion(existing, file))
                    .forEach(merged::add);
        }
        return new AssociatedArtifactList(merged.stream().distinct().toList());
    }

    private static Collection<AssociatedArtifact> filterOutOpenFilesWithPublishedVersion(AssociatedArtifactList artifacts) {
        return artifacts.stream()
                .filter(associatedArtifact -> !(associatedArtifact instanceof OpenFile openFile && hasPublishedVersion(openFile)))
                .toList();
    }

    private static boolean hasPublishedVersion(OpenFile openFile) {
        return PUBLISHED_VERSION.equals(openFile.getPublisherVersion());
    }

    private static boolean isUniqueOpenFileWithPublishedVersion(AssociatedArtifactList existing, File file) {
        return PUBLISHED_VERSION.equals(file.getPublisherVersion())
                && existing.stream()
                .noneMatch(associatedArtifact -> associatedArtifact instanceof OpenFile openFile
                        && hasPublishedVersion(openFile)
                        && openFile.getName().equals(file.getName()));
    }

    private static boolean hasMultipleOrNoOpenFilesWithPublishedVersion(AssociatedArtifactList artifacts) {
        long count = artifacts.stream()
                .filter(File.class::isInstance)
                .map(File.class::cast)
                .filter(OpenFile.class::isInstance)
                .filter(f -> PUBLISHED_VERSION.equals(f.getPublisherVersion()))
                .count();
        return count != SINGLETON;
    }
}
