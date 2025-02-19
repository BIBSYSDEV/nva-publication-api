package no.unit.nva.publication.permissions.file.grant;

import no.unit.nva.model.FileOperation;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileGrantStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public class EmbargoCuratorFileGrantStrategy extends FileStrategyBase implements FileGrantStrategy {

    public EmbargoCuratorFileGrantStrategy(FileEntry file, UserInstance userInstance, Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean allowsAction(FileOperation permission) {
        return fileIsFinalized() && fileHasEmbargo() && !resourceIsDegree() && currentUserIsFileCurator();
    }
}
