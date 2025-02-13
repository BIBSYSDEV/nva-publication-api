package no.unit.nva.publication.permissions.file;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
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
        return hasAccessRight(AccessRight.MANAGE_RESOURCE_FILES);
    }

    protected boolean currentUserIsContributor() {
        return nonNull(userInstance)
               && nonNull(this.userInstance.getPersonCristinId())
               && Optional.ofNullable(resource.getEntityDescription())
                      .map(EntityDescription::getContributors)
                      .stream()
                      .flatMap(List::stream)
                      .filter(this::isVerifiedContributor)
                      .anyMatch(
                          contributor -> contributor.getIdentity().getId().equals(this.userInstance.getPersonCristinId()));
    }


    private boolean isVerifiedContributor(Contributor contributor) {
        return contributor.getIdentity() != null && contributor.getIdentity().getId() != null;
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
}
