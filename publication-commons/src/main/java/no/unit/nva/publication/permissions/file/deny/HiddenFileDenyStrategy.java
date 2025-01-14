package no.unit.nva.publication.permissions.file.deny;

import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileDenyStrategy;

public class HiddenFileDenyStrategy extends FileDenyStrategy {

    public HiddenFileDenyStrategy(File file,
                                  UserInstance userInstance) {
        super(file, userInstance);
    }

    @Override
    public boolean deniesAction(FileOperation permission) {
        return file instanceof HiddenFile;//TODO add exception of curator on same institution
    }
}
