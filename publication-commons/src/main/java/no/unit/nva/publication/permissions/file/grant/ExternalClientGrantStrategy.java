package no.unit.nva.publication.permissions.file.grant;

import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileGrantStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public class ExternalClientGrantStrategy extends FileStrategyBase implements FileGrantStrategy {

    public ExternalClientGrantStrategy(FileEntry file,
                                       UserInstance userInstance,
                                       Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean allowsAction(FileOperation permission) {
        return isExternalClientWithRelation() && isAllowedFileType();
    }

    private boolean isAllowedFileType() {
        return file.getFile() instanceof OpenFile || file.getFile() instanceof InternalFile;
    }
}
