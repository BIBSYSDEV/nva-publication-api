package no.sikt.nva.brage.migration.merger;

import static java.util.function.Predicate.not;
import static no.unit.nva.model.associatedartifacts.file.PublisherVersion.PUBLISHED_VERSION;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;

public final class AssociatedArtifactsMerger {

    private AssociatedArtifactsMerger() {
    }

    public static AssociatedArtifactList merge(AssociatedArtifactList existing, AssociatedArtifactList incoming) {
        var associatedArtifacts = new ArrayList<AssociatedArtifact>();
        associatedArtifacts.addAll(getAssociatedArtifactsExceptOpenFiles(existing));
        associatedArtifacts.addAll(getAssociatedArtifactsExceptOpenFiles(incoming));

        if (hasNoOpenFiles(existing)) {
            associatedArtifacts.addAll(getOpenFiles(incoming));
        } else if (hasNoOpenFilesWithPublishedVersion(existing)) {
            associatedArtifacts.addAll(convertOpenFilesToInternal(existing));
            associatedArtifacts.addAll(getOpenFilesWithPublishedVersion(incoming));
        } else if (hasOpenFilesWithPublishedVersion(existing)) {
            associatedArtifacts.addAll(getOpenFiles(existing));
            associatedArtifacts.addAll(
                getOpenFilesWithPublishedVersionAndDifferentNamesThanExisting(existing, incoming));
        }
        return new AssociatedArtifactList(associatedArtifacts);
    }

    private static boolean hasOpenFilesWithPublishedVersion(AssociatedArtifactList existing) {
        return existing.stream().anyMatch(AssociatedArtifactsMerger::isOpenFileWithPublishedVersion);
    }

    private static boolean hasNoOpenFilesWithPublishedVersion(AssociatedArtifactList existing) {
        return existing.stream().noneMatch(AssociatedArtifactsMerger::isOpenFileWithPublishedVersion);
    }

    private static boolean hasNoOpenFiles(AssociatedArtifactList existing) {
        return existing.stream().noneMatch(OpenFile.class::isInstance);
    }

    private static List<InternalFile> convertOpenFilesToInternal(AssociatedArtifactList associatedArtifactList) {
        return getOpenFiles(associatedArtifactList).stream().map(File::toInternalFile).toList();
    }

    private static List<AssociatedArtifact> getOpenFilesWithPublishedVersion(
        AssociatedArtifactList associatedArtifactList) {
        return associatedArtifactList.stream()
                   .filter(AssociatedArtifactsMerger::isOpenFileWithPublishedVersion)
                   .toList();
    }

    private static List<File> getOpenFiles(AssociatedArtifactList associatedArtifactList) {
        return associatedArtifactList.stream().filter(OpenFile.class::isInstance).map(File.class::cast).toList();
    }

    private static List<AssociatedArtifact> getOpenFilesWithPublishedVersionAndDifferentNamesThanExisting(
        AssociatedArtifactList existing, AssociatedArtifactList incoming) {
        var associatedArtifacts = new ArrayList<AssociatedArtifact>();
        for (AssociatedArtifact incomingArtifact : incoming) {
            if (isOpenFileWithPublishedVersion(incomingArtifact) &&
                thereIsNoOpenFileWithPublishedVersionInCollectionWithTheSameFilename(existing, incomingArtifact)) {
                associatedArtifacts.add(incomingArtifact);
            }
        }
        return associatedArtifacts;
    }

    private static boolean thereIsNoOpenFileWithPublishedVersionInCollectionWithTheSameFilename(
        AssociatedArtifactList existing, AssociatedArtifact incomingArtifact) {
        return existing.stream()
                   .filter(AssociatedArtifactsMerger::isOpenFileWithPublishedVersion)
                   .map(OpenFile.class::cast)
                   .noneMatch(existingFile -> existingFile.getName().equals(getFilename(incomingArtifact)));
    }

    private static String getFilename(AssociatedArtifact incomingArtifact) {
        return ((File) incomingArtifact).getName();
    }

    private static boolean isOpenFileWithPublishedVersion(AssociatedArtifact associatedArtifact) {
        return associatedArtifact instanceof OpenFile openFile &&
               PUBLISHED_VERSION.equals(openFile.getPublisherVersion());
    }

    private static List<AssociatedArtifact> getAssociatedArtifactsExceptOpenFiles(
        AssociatedArtifactList associatedArtifactList) {
        return associatedArtifactList.stream().filter(not(OpenFile.class::isInstance)).toList();
    }
}
