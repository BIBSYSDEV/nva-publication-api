package no.unit.nva.publication.permissions.file;

import static java.util.Objects.nonNull;
import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
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

    protected boolean hasTopLevelRelationForCurrentResource() {
        var userTopLevelOrg = userInstance.getTopLevelOrgCristinId();

        logger.info("checking if resource top level affiliation {} for user {} is equal to {}.",
                    resource.getCuratingInstitutions().stream().map(CuratingInstitution::id).map(
                        URI::toString).collect(Collectors.joining(", ")),
                    userInstance.getUser(),
                    userTopLevelOrg);

        return resource.getCuratingInstitutions().stream().anyMatch(org -> org.id().equals(userTopLevelOrg));
    }

    protected boolean currentUserIsFileCurator() {
        return hasAccessRight(AccessRight.MANAGE_RESOURCE_FILES) && hasTopLevelRelationForCurrentResource();
    }

    protected boolean currentUserIsContributor() {
        return Optional.ofNullable(userInstance)
                   .map(UserInstance::getPersonCristinId)
                   .map(this::isVerifiedContributorAtResource)
                   .orElse(false);
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
        return nonNull(userInstance) &&
               userInstance.isExternalClient() &&
               userInstance.getCustomerId().equals(resource.getPublisher().getId());
    }

    protected boolean isDegree() {
        return Optional.ofNullable(resource.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .map(FileStrategyBase::publicationInstanceIsDegree)
                   .orElse(false);
    }

    private static Boolean publicationInstanceIsDegree(PublicationInstance<? extends Pages> publicationInstance) {
        return Arrays.stream(PROTECTED_DEGREE_INSTANCE_TYPES)
                   .anyMatch(instanceTypeClass -> instanceTypeClass.equals(publicationInstance.getClass()));
    }

    public boolean isEmbargo() {
        return !file.getFile().fileDoesNotHaveActiveEmbargo();
    }

    protected boolean currentUserIsDegreeEmbargoFileCuratorForGivenFile() {
        return hasAccessRight(MANAGE_DEGREE_EMBARGO) && hasTopLevelRelationForCurrentResource();
    }

    protected boolean currentUserIsDegreeFileCuratorForGivenFile() {
        return hasAccessRight(MANAGE_DEGREE) && haveTopLevelRelationForCurrentFile();
    }
}
