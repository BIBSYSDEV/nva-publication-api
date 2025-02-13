package no.unit.nva.publication.permissions.file.grant;

import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileGrantStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public class FileOwnerGrantStrategy extends FileStrategyBase implements FileGrantStrategy {

    public FileOwnerGrantStrategy(FileEntry file, UserInstance userInstance, Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean allowsAction(FileOperation permission) {
        if (currentUserIsFileOwner()) {
            return switch (permission) {
                case READ_METADATA, DOWNLOAD -> !(file.getFile() instanceof HiddenFile);
                case WRITE_METADATA, DELETE -> !fileIsFinalized();
            };
        }

        return false;
    }

}
