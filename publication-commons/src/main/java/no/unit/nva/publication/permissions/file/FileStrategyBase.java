package no.unit.nva.publication.permissions.file;

import static java.util.Objects.nonNull;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileStrategyBase {

    public static final Logger logger = LoggerFactory.getLogger(FileStrategyBase.class);

    protected final FileEntry file;
    protected final UserInstance userInstance;
    protected final Resource resource;

    protected FileStrategyBase(FileEntry file, UserInstance userInstance, Resource resource) {
        this.file = file;
        this.userInstance = userInstance;
        this.resource = resource;
    }

    protected boolean hasAccessRight(AccessRight accessRight) {
        return nonNull(userInstance) && userInstance.getAccessRights().contains(accessRight);
    }

    protected boolean currentUserIsFileCuratorForGivenFile() {
        return currentUserIsFileCurator() && isFileCuratorForCurrentOrganization();
    }

    private boolean isFileCuratorForCurrentOrganization() {
        var userTopLevelOrg = userInstance.getTopLevelOrgCristinId();

        logger.info("checking if file top level affiliation {} for user {} is equal to {}.",
                    file.getOwnerAffiliation(),
                    userInstance.getUser(),
                    userTopLevelOrg);

        return file.getOwnerAffiliation().equals(userTopLevelOrg);
    }

    protected boolean currentUserIsFileCurator() {
        return nonNull(userInstance) && hasAccessRight(AccessRight.MANAGE_RESOURCE_FILES);
    }
}
