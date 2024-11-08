package no.unit.nva.publication.model.business;

import static nva.commons.core.attempt.Try.attempt;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.associatedartifacts.file.File;

public class AcceptedPublishingRequestMigrator {

    private AcceptedPublishingRequestMigrator() {
    }

    @Deprecated
    public static Set<File> migrateApprovedFiles(Set<Object> approvedFiles) {
        if (EveryEntryIsIdentifier(approvedFiles)) {
            return migrateIdentifiers(approvedFiles);
        } else if (everyEntryIsFile(approvedFiles)) {
            return migrateFiles(approvedFiles);
        } else if (everyEntryIsMap(approvedFiles)) {
            return migrateFiles(approvedFiles);
        } else {
            return Set.of();
        }
    }

    @Deprecated
    public static Set<File> migrateFilesForApproval(Set<Object> filesForApproval) {
        if (everyEntryIsFileForApproval(filesForApproval)) {
            return migrateFilesForApprovals(filesForApproval);
        } else if (everyEntryIsFile(filesForApproval)) {
            return migrateFiles(filesForApproval);
        } else if (isSingletonMap(filesForApproval)) {
            return migrateFilesForApprovals(filesForApproval);
        } else if (everyEntryIsMap(filesForApproval)) {
            return migrateFiles(filesForApproval);
        } else {
            return Set.of();
        }
    }

    private static boolean EveryEntryIsIdentifier(Set<Object> approvedFiles) {
        return approvedFiles.stream().allMatch(value -> value instanceof String || value instanceof UUID);
    }

    private static boolean everyEntryIsMap(Set<Object> approvedFiles) {
        return approvedFiles.stream().allMatch(Map.class::isInstance);
    }

    private static boolean everyEntryIsFileForApproval(Set<Object> filesForApproval) {
        return filesForApproval.stream().allMatch(FileForApproval.class::isInstance);
    }

    private static boolean everyEntryIsFile(Set<Object> filesForApproval) {
        return filesForApproval.stream().allMatch(File.class::isInstance);
    }

    private static boolean isSingletonMap(Set<Object> filesForApproval) {
        return filesForApproval.stream().allMatch(file -> file instanceof Map<?, ?> map && map.size() == 1);
    }

    private static Set<File> migrateFilesForApprovals(Set<Object> filesForApproval) {
        return filesForApproval.stream()
                   .map(map -> attempt(
                       () -> JsonUtils.dtoObjectMapper.convertValue(map, FileForApproval.class)).toOptional())
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .map(fileForApproval -> toOpenFileWithIdentifier(fileForApproval.identifier().toString()))
                   .collect(Collectors.toSet());
    }

    private static Set<File> migrateFiles(Set<Object> approvedFiles) {
        return approvedFiles.stream()
                   .map(map -> attempt(() -> JsonUtils.dtoObjectMapper.convertValue(map, File.class)).toOptional())
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .collect(Collectors.toSet());
    }

    private static Set<File> migrateIdentifiers(Set<Object> approvedFiles) {
        return approvedFiles.stream()
                   .map(fileIdentifier -> toOpenFileWithIdentifier(fileIdentifier.toString()))
                   .collect(Collectors.toSet());
    }

    private static File toOpenFileWithIdentifier(String fileIdentifier) {
        return File.builder().withIdentifier(UUID.fromString(fileIdentifier)).buildOpenFile();
    }
}
