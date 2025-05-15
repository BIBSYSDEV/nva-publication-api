package no.unit.nva.publication.permissions.publication;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.model.associatedartifacts.file.File.ACCEPTED_FILE_TYPES;
import java.util.Arrays;
import java.util.Optional;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicationStrategyBase {

    public static final Logger logger = LoggerFactory.getLogger(PublicationStrategyBase.class);

    protected final Resource resource;
    protected final UserInstance userInstance;

    protected PublicationStrategyBase(Resource resource, UserInstance userInstance) {
        this.resource = resource;
        this.userInstance = userInstance;
    }

    protected boolean hasAccessRight(AccessRight accessRight) {
        return nonNull(userInstance) && userInstance.getAccessRights().contains(accessRight);
    }

    protected boolean isProtectedDegreeInstanceType() {
        return Optional.ofNullable(resource.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .map(PublicationStrategyBase::publicationInstanceIsDegree)
                   .orElse(false);
    }

    protected boolean userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution() {
        return userIsFromSameInstitutionAsPublicationOwner() || userBelongsToCuratingInstitution();
    }

    private boolean userBelongsToCuratingInstitution() {
        if (isNull(userInstance)) {
            return false;
        }

        var userTopLevelOrg = userInstance.getTopLevelOrgCristinId();

        logger.info("found topLevels {} for user {} of {}.",
                    resource.getCuratingInstitutions(),
                    userInstance.getUser(),
                    userTopLevelOrg);
        return resource.getCuratingInstitutions().stream().anyMatch(org -> org.id().equals(userTopLevelOrg));
    }

    protected boolean userIsFromSameInstitutionAsPublicationOwner() {
        if (isNull(userInstance) || isNull(userInstance.getTopLevelOrgCristinId()) || isNull(resource.getResourceOwner())) {
            return false;
        }

        return userInstance.getTopLevelOrgCristinId().equals(resource.getResourceOwner().getOwnerAffiliation());
    }

    protected boolean isProtectedDegreeInstanceTypeWithEmbargo() {
        return isProtectedDegreeInstanceType() && resource.getAssociatedArtifacts().stream()
                                                      .filter(File.class::isInstance)
                                                      .map(File.class::cast)
                                                      .anyMatch(this::hasEmbargo);
    }

    private boolean hasEmbargo(File file) {
        return file.hasActiveEmbargo();
    }

    protected boolean isVerifiedContributor(Contributor contributor) {
        return Optional.ofNullable(contributor.getIdentity()).map(Identity::getId).isPresent();
    }

    protected boolean hasApprovedFiles() {
        return resource.getAssociatedArtifacts()
                   .stream()
                   .anyMatch(artifact -> ACCEPTED_FILE_TYPES
                                             .contains(artifact.getClass()));
    }

    protected boolean hasOpenFiles() {
        return resource.getAssociatedArtifacts()
                   .stream()
                   .anyMatch(OpenFile.class::isInstance);
    }

    protected boolean isOwner() {
        var owner = UserInstance.fromPublication(resource.toPublication()).getUsername();
        return Optional.ofNullable(userInstance)
                   .map(userInstance -> owner.equals(userInstance.getUsername()))
                   .orElse(false);
    }

    protected boolean isUsersDraft() {
        return isDraft() && isOwner();
    }

    protected boolean isDraft() {
        return resource.getStatus().equals(DRAFT);
    }

    protected boolean isUnpublished() {
        return resource.getStatus().equals(UNPUBLISHED);
    }

    protected boolean isPublished() {
        return resource.getStatus().equals(PUBLISHED);
    }

    private static Boolean publicationInstanceIsDegree(PublicationInstance<? extends Pages> publicationInstance) {
        return Arrays.stream(PROTECTED_DEGREE_INSTANCE_TYPES)
                   .anyMatch(instanceTypeClass -> instanceTypeClass.equals(publicationInstance.getClass()));
    }
}
