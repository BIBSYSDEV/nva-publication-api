package no.unit.nva.publication.permissions.file.grant;

import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileGrantStrategy;

public class EveryoneGrantStrategy extends FileGrantStrategy {

    public EveryoneGrantStrategy(File file,
                                    UserInstance userInstance) {
        super(file, userInstance);
    }

    @Override
    public boolean allowsAction(FileOperation permission) {
        if (FileOperation.READ_METADATA == permission) {//TODO
            return file instanceof OpenFile;
        }
        return false;
    }
}
