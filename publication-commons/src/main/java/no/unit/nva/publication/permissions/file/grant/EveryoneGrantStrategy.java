package no.unit.nva.publication.permissions.file.grant;

import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileGrantStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public final class EveryoneGrantStrategy extends FileStrategyBase implements FileGrantStrategy {

    public EveryoneGrantStrategy(File file,
                                    UserInstance userInstance) {
        super(file, userInstance);
    }

    @Override
    public boolean allowsAction(FileOperation permission) {
        if (file instanceof OpenFile openFile) {
            return switch (permission) {
                case READ_METADATA -> true;
                case DOWNLOAD -> openFile.fileDoesNotHaveActiveEmbargo();
                default -> false;
            };
        }

        return false;
    }
}
