package no.unit.nva.publication.permissions.file;

import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.business.UserInstance;

public abstract class FileDenyStrategy extends FileStrategyBase {

    protected FileDenyStrategy(File file, UserInstance userInstance) {
        super(file, userInstance);
    }

    public abstract boolean deniesAction(FileOperation permission);
}
