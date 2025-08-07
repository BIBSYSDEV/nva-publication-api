package no.unit.nva.publication.permissions.file.deny;

import static no.unit.nva.model.FileOperation.DOWNLOAD;
import static no.unit.nva.model.FileOperation.READ_METADATA;
import java.util.List;
import no.unit.nva.model.FileOperation;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileDenyStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public class EmbargoReadDenyStrategy extends FileStrategyBase implements FileDenyStrategy {

    public EmbargoReadDenyStrategy(FileEntry file,
                                   UserInstance userInstance,
                                   Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean deniesAction(FileOperation permission) {
        if (!fileHasEmbargo() || !List.of(READ_METADATA, DOWNLOAD).contains(permission)) {
            return false;
        }

        if (shouldAllowMetadataReadOnFinalizedFiles(permission)) {
            return false;
        }

        return isDeniedUser();
    }

    private boolean shouldAllowMetadataReadOnFinalizedFiles(FileOperation permission) {
        return permission == READ_METADATA && (fileIsFinalized() || userRelatesToPublication());
    }

    private boolean isDeniedUser() {
        if (currentUserIsFileOwner()) {
            return false;
        }

        return resourceIsDegree()
                   ? isDegreeEmbargoDeniedUser()
                   : isEmbargoDeniedUser();
    }

    private boolean isDegreeEmbargoDeniedUser() {
        return !(currentUserIsDegreeEmbargoFileCuratorForGivenFile()
                 || isExternalClientWithRelation());
    }

    private boolean isEmbargoDeniedUser() {
        return !(currentUserIsFileOwner()
                 || currentUserIsContributor()
                 || currentUserIsFileCuratorForGivenFile()
                 || currentUserIsFileCurator()
                 || isExternalClientWithRelation());
    }
}
