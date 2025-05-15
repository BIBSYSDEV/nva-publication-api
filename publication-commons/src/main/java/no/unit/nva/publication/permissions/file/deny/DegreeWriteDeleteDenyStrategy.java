package no.unit.nva.publication.permissions.file.deny;

import no.unit.nva.model.FileOperation;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileDenyStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public class DegreeWriteDeleteDenyStrategy extends FileStrategyBase implements FileDenyStrategy {

    public DegreeWriteDeleteDenyStrategy(FileEntry file,
                                         UserInstance userInstance,
                                         Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean deniesAction(FileOperation permission) {
        return resourceIsDegree() && !fileHasEmbargo() && isWriteOrDelete(permission) && isDeniedUser();
    }

    private boolean isDeniedUser() {
        return !(currentUserIsDegreeFileCuratorForGivenFile()
                 || currentUserIsDegreeEmbargoFileCuratorForGivenFile()
                 || isExternalClientWithRelation());
    }
}
