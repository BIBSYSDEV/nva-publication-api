package no.unit.nva.publication.permissions.file.deny;

import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileDenyStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public final class HiddenFileDenyStrategy extends FileStrategyBase implements FileDenyStrategy {

    public HiddenFileDenyStrategy(FileEntry file, UserInstance userInstance, Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean deniesAction(FileOperation permission) {
        if (currentUserIsFileCuratorForGivenFile()) {
            return false;//allow curator to access hidden files
        }

        return file.getFile() instanceof HiddenFile;
    }
}
