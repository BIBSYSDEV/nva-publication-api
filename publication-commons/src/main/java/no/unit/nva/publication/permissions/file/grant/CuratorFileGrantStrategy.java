package no.unit.nva.publication.permissions.file.grant;

import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import no.unit.nva.model.FileOperation;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileGrantStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public class CuratorFileGrantStrategy extends FileStrategyBase implements FileGrantStrategy {

    public CuratorFileGrantStrategy(FileEntry file, UserInstance userInstance, Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean allowsAction(FileOperation permission) {
        if (hasAccessRight(MANAGE_RESOURCES_STANDARD)) {
            return switch (permission) {
                case READ_METADATA, DOWNLOAD -> currentUserIsCuratorForGivenFile();
                case WRITE_METADATA, DELETE -> false;
            };
        }

        return false;
    }
}
