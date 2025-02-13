package no.unit.nva.publication.permissions.file;

import static java.util.Objects.nonNull;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import static nva.commons.core.attempt.Try.attempt;
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
        return currentUserIsFileCurator() && haveTopLevelRelationForCurrentFile();
    }

    private boolean haveTopLevelRelationForCurrentFile() {
        var userTopLevelOrg = userInstance.getTopLevelOrgCristinId();

        logger.info("checking if file top level affiliation {} for user {} is equal to {}.",
                    file.getOwnerAffiliation(),
                    userInstance.getUser(),
                    userTopLevelOrg);

        return file.getOwnerAffiliation().equals(userTopLevelOrg);
    }

    private boolean haveTopLevelRelationForCurrentResource() {
        var userTopLevelOrg = userInstance.getTopLevelOrgCristinId();

        logger.info("checking if resource top level affiliation {} for user {} is equal to {}.",
                    resource.getCuratingInstitutions().stream().map(CuratingInstitution::id).map(
                        URI::toString).collect(Collectors.joining(", ")),
                    userInstance.getUser(),
                    userTopLevelOrg);

        return resource.getCuratingInstitutions().stream().anyMatch(org -> org.id().equals(userTopLevelOrg));
    }

    protected boolean currentUserIsFileCurator() {
        return hasAccessRight(AccessRight.MANAGE_RESOURCE_FILES) && haveTopLevelRelationForCurrentResource();
    }

    protected boolean currentUserIsFileOwner() {
        return Optional.ofNullable(userInstance)
                   .map(UserInstance::getUser)
                   .map(user -> user.equals(file.getOwner()))
                   .orElse(false);
    }

    protected boolean fileIsFinalized() {
        return file.getFile() instanceof OpenFile
               || file.getFile() instanceof InternalFile
               || file.getFile() instanceof HiddenFile;
    }

    protected boolean isExternalClientWithRelation() {
        return nonNull(userInstance)
               && userInstance.isExternalClient()
               && attempt(
            () -> userInstance.getCustomerId().equals(resource.getPublisher().getId()))
                      .orElse(fail -> false);
    }
}
