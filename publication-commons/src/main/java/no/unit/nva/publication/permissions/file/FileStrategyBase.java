package no.unit.nva.publication.permissions.file;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.FileOperation.DELETE;
import static no.unit.nva.model.FileOperation.DOWNLOAD;
import static no.unit.nva.model.FileOperation.WRITE_METADATA;
import static no.unit.nva.model.associatedartifacts.file.File.FINALIZED_FILE_TYPES;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.FileOperation;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
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

    public boolean fileHasEmbargo() {
        return file.getFile().hasActiveEmbargo();
    }

    protected boolean hasAccessRight(AccessRight accessRight) {
        return nonNull(userInstance) && userInstance.getAccessRights().contains(accessRight);
    }

    protected boolean currentUserIsFileCuratorForGivenFile() {
        return hasAccessRight(AccessRight.MANAGE_RESOURCE_FILES) &&
               (haveTopLevelRelationForCurrentFile() || userBelongsToPublicationChannelOwner());
    }

    protected boolean currentUserIsFileCurator() {
        return hasAccessRight(AccessRight.MANAGE_RESOURCE_FILES) && userRelatesToPublication();
    }

    protected boolean currentUserIsContributor() {
        return Optional.ofNullable(userInstance)
                   .map(UserInstance::getPersonCristinId)
                   .map(this::isVerifiedContributorAtResource)
                   .orElse(false);
    }

    protected boolean currentUserIsFileOwner() {
        return Optional.ofNullable(userInstance)
                   .map(UserInstance::getUser)
                   .map(user -> user.equals(file.getOwner()))
                   .orElse(false);
    }

    protected boolean fileIsFinalized() {
        return FINALIZED_FILE_TYPES.contains(file.getFile().getClass());
    }

    protected boolean isExternalClientWithRelation() {
        return nonNull(userInstance) && userInstance.isExternalClient() &&
               userInstance.getCustomerId().equals(resource.getPublisher().getId());
    }

    protected boolean resourceIsDegree() {
        return resource.isDegree();
    }

    protected boolean currentUserIsDegreeEmbargoFileCuratorForGivenFile() {
        return hasAccessRight(MANAGE_DEGREE_EMBARGO) && (haveTopLevelRelationForCurrentFile() || userBelongsToPublicationChannelOwner());
    }

    protected boolean currentUserIsDegreeFileCuratorForGivenFile() {
        return hasAccessRight(MANAGE_DEGREE) && (haveTopLevelRelationForCurrentFile() || userBelongsToPublicationChannelOwner());
    }

    protected boolean isWriteOrDelete(FileOperation permission) {
        return permission.equals(WRITE_METADATA) || permission.equals(DELETE);
    }

    protected boolean isWriteOrDeleteOrDownload(FileOperation permission) {
        return permission.equals(WRITE_METADATA) || permission.equals(DELETE) || permission.equals(DOWNLOAD);
    }

    private boolean haveTopLevelRelationForCurrentFile() {
        var userTopLevelOrg = userInstance.getTopLevelOrgCristinId();

        logger.info("checking if file top level affiliation {} for user {} is equal to {}.", file.getOwnerAffiliation(),
                    userInstance.getUser(), userTopLevelOrg);

        return file.getOwnerAffiliation().equals(userTopLevelOrg);
    }

    private boolean userBelongsToCuratingInstitution() {
        var userTopLevelOrg = userInstance.getTopLevelOrgCristinId();

        logger.info("checking if resource top level affiliation {} for user {} is equal to {}.",
                    resource.getCuratingInstitutions()
                        .stream()
                        .map(CuratingInstitution::id)
                        .map(URI::toString)
                        .collect(Collectors.joining(", ")), userInstance.getUser(), userTopLevelOrg);

        return resource.getCuratingInstitutions().stream().anyMatch(org -> org.id().equals(userTopLevelOrg));
    }

    public boolean userRelatesToPublication() {
        return userIsFromSameInstitutionAsPublicationOwner() || userBelongsToCuratingInstitution() ||
               userBelongsToPublicationChannelOwner();
    }

    protected boolean userBelongsToPublicationChannelOwner() {
        if (Optional.ofNullable(userInstance).map(UserInstance::getTopLevelOrgCristinId).isEmpty()) {
            return false;
        }

        return resource.getPrioritizedClaimedPublicationChannelWithinScope()
                   .map(ClaimedPublicationChannel::getOrganizationId)
                   .map(userInstance.getTopLevelOrgCristinId()::equals)
                   .orElse(false);
    }

    private boolean userIsFromSameInstitutionAsPublicationOwner() {
        return resource.getResourceOwner().getOwnerAffiliation().equals(userInstance.getTopLevelOrgCristinId());
    }

    private boolean isVerifiedContributorAtResource(URI personCristinId) {
        return Optional.ofNullable(resource.getEntityDescription())
                   .map(EntityDescription::getContributors)
                   .stream()
                   .flatMap(List::stream)
                   .filter(this::isVerifiedContributor)
                   .anyMatch(contributor -> contributor.getIdentity().getId().equals(personCristinId));
    }

    private boolean isVerifiedContributor(Contributor contributor) {
        return nonNull(contributor.getIdentity()) && contributor.getIdentity().getId() != null;
    }
}
