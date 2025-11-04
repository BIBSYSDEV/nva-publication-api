package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;

public enum FilesStatus {

    NO_FILES("noFiles"), HAS_PUBLIC_FILES("hasPublicFiles");

    public static final String FILES_STATUS = "filesStatus";
    private final String value;

    FilesStatus(String value) {
        this.value = value;
    }

    public static FilesStatus fromAssociatedArtifacts(AssociatedArtifactList associatedArtifacts) {
        var files = getFiles(associatedArtifacts);
        if (noFiles(files) || !containsVisibleForNonOwnerFile(files)) {
            return NO_FILES;
        } else {
            return HAS_PUBLIC_FILES;
        }
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    private static boolean noFiles(List<File> files) {
        return files.isEmpty();
    }

    private static boolean containsVisibleForNonOwnerFile(List<File> files) {
        return files.stream().anyMatch(File::isVisibleForNonOwner);
    }

    private static List<File> getFiles(AssociatedArtifactList associatedArtifacts) {
        return associatedArtifacts.stream()
                   .filter(File.class::isInstance)
                   .map(File.class::cast)
                   .toList();
    }
}
