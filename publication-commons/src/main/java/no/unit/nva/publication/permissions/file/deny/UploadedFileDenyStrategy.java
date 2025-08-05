package no.unit.nva.publication.permissions.file.deny;

import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.UploadedFile;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileDenyStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public class UploadedFileDenyStrategy extends FileStrategyBase implements FileDenyStrategy {

    public UploadedFileDenyStrategy(FileEntry file,
                                    UserInstance userInstance,
                                    Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean deniesAction(FileOperation permission) {
        if (!fileTypeIsUploadedFile()) {
            return false;
        }

        if (currentUserIsFileOwner()) {
            return false;
        }

        if (resourceIsDegree() && isWriteOrDelete(permission)) {
            return !currentUserIsDegreeFileCuratorForGivenFile();
        }

        return isDeniedUser();
    }

    private boolean isDeniedUser() {
        return !(currentUserIsFileCuratorForGivenFile()
                 || currentUserIsDegreeFileCuratorForGivenFile()
                 || currentUserIsSupportCuratorForGivenFile());
    }

    private boolean fileTypeIsUploadedFile() {
        return file.getFile() instanceof UploadedFile;
    }
}
