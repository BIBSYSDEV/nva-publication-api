package no.unit.nva.publication.model.business.publicationstate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.ImportSource.Source;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.ThirdPartySystem;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.FileLogEntry;
import no.unit.nva.publication.model.business.logentry.LogAgent;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import nva.commons.core.JacocoGenerated;

public record FileUploadedEvent(Instant date, User user, URI institution, SortableIdentifier identifier,
                                ImportSource importSource) implements FileEvent {

    public static FileUploadedEvent create(UserInstance userInstance, Instant timestamp) {
        return new FileUploadedEvent(timestamp, userInstance.getUser(), userInstance.getTopLevelOrgCristinId(),
                                     SortableIdentifier.next(),
                                     userInstance.isExternalClient() ? getImportSource(userInstance) : null);
    }

    private static ImportSource getImportSource(UserInstance userInstance) {
        return userInstance.getThirdPartySystem()
                   .map(FileUploadedEvent::toSource)
                   .map(ImportSource::fromSource)
                   .orElse(ImportSource.fromSource(Source.OTHER));

    }

    @JacocoGenerated
    private static Source toSource(ThirdPartySystem thirdPartySystem) {
        return switch (thirdPartySystem) {
            case INSPERA -> Source.INSPERA;
            case WISE_FLOW -> Source.CRISTIN;
            case OTHER -> Source.OTHER;
        };
    }

    @Override
    public FileLogEntry toLogEntry(FileEntry fileEntry, LogAgent user) {
        return FileLogEntry.builder()
                   .withIdentifier(identifier)
                   .withFileIdentifier(fileEntry.getIdentifier())
                   .withResourceIdentifier(fileEntry.getResourceIdentifier())
                   .withTopic(LogTopic.FILE_UPLOADED)
                   .withTimestamp(date)
                   .withPerformedBy(user)
                   .withFilename(fileEntry.getFile().getName())
                   .withFileType(fileEntry.getFile().getClass().getSimpleName())
                   .withImportSource(importSource)
                   .build();
    }
}
