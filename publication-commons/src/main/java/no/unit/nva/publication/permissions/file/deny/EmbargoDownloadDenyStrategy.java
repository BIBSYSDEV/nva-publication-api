package no.unit.nva.publication.permissions.file.deny;

import static no.unit.nva.model.FileOperation.READ_METADATA;
import no.unit.nva.model.FileOperation;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileDenyStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public class EmbargoDownloadDenyStrategy extends FileStrategyBase implements FileDenyStrategy {

    public EmbargoDownloadDenyStrategy(FileEntry file,
                                       UserInstance userInstance,
                                       Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean deniesAction(FileOperation permission) {
        if (!fileHasEmbargo()) {
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
