package no.unit.nva.publication.permissions.file.grant;

import no.unit.nva.model.FileOperation;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileGrantStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public class ContributorFileGrantStrategy extends FileStrategyBase implements FileGrantStrategy {

    public ContributorFileGrantStrategy(FileEntry file,
                                        UserInstance userInstance,
                                        Resource resource) {
        super(file, userInstance, resource);
    }

    @Override
    public boolean allowsAction(FileOperation permission) {

        return switch (permission) {
            case READ_METADATA, DOWNLOAD -> currentUserIsContributor();
            case WRITE_METADATA, DELETE -> false;
        };
    }
}
