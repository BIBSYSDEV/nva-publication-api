package no.unit.nva.publication.permissions.file.deny;

import static no.unit.nva.model.FileOperation.DOWNLOAD;
import no.unit.nva.model.FileOperation;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileDenyStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public class EmbargoDownloadDenyStrategy extends FileStrategyBase implements FileDenyStrategy {

    public static final boolean PASS = false;

    public EmbargoDownloadDenyStrategy(FileEntry file,
                                       UserInstance userInstance,
                                       Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean deniesAction(FileOperation permission) {
        if (permission.equals(DOWNLOAD) && isEmbargo()) {
            return isDenied();
        }
        return PASS;
    }

    private boolean isDenied() {
        if (isDegree()) {
            return isDegreeEmbargoDeniedUser();
        } else {
            return isEmbargoDeniedUser();
        }
    }

    private boolean isDegreeEmbargoDeniedUser() {
        return !(currentUserIsDegreeEmbargoFileCuratorForGivenFile() || currentUserIsFileOwner());
    }

    private boolean isEmbargoDeniedUser() {
        return !(currentUserIsFileOwner()
                 || currentUserIsContributor()
                 || currentUserIsFileCuratorForGivenFile()
                 || currentUserIsDegreeEmbargoFileCuratorForGivenFile()
                 || currentUserIsFileCurator());
    }
}
