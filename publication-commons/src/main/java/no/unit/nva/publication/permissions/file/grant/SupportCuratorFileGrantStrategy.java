package no.unit.nva.publication.permissions.file.grant;

import static nva.commons.apigateway.AccessRight.SUPPORT;
import no.unit.nva.model.FileOperation;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileGrantStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public class SupportCuratorFileGrantStrategy extends FileStrategyBase implements FileGrantStrategy {

    public SupportCuratorFileGrantStrategy(FileEntry file, UserInstance userInstance, Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean allowsAction(FileOperation permission) {
        if (hasAccessRight(SUPPORT)) {
            return switch (permission) {
                case READ_METADATA, DOWNLOAD -> userRelatesToPublication();
                case WRITE_METADATA, DELETE -> false;
            };
        }

        return false;
    }
}
