package no.unit.nva.publication.permissions.publication;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.model.associatedartifacts.file.File.ACCEPTED_FILE_TYPES;
import static nva.commons.core.attempt.Try.attempt;
import java.util.Arrays;
import java.util.Optional;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicationStrategyBase {

    public static final Logger logger = LoggerFactory.getLogger(PublicationStrategyBase.class);

    protected final Publication publication;
    protected final UserInstance userInstance;

    protected PublicationStrategyBase(Publication publication, UserInstance userInstance) {
        this.publication = publication;
        this.userInstance = userInstance;
    }

    protected boolean hasAccessRight(AccessRight accessRight) {
        return nonNull(userInstance) && userInstance.getAccessRights().contains(accessRight);
    }

    protected boolean isProtectedDegreeInstanceType() {
        return Optional.ofNullable(publication.getEntityDescription())
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
                    publication.getCuratingInstitutions(),
                    userInstance.getUser(),
                    userTopLevelOrg);
        return publication.getCuratingInstitutions().stream().anyMatch(org -> org.id().equals(userTopLevelOrg));
    }

    protected boolean userIsFromSameInstitutionAsPublicationOwner() {
        if (isNull(userInstance) || isNull(userInstance.getTopLevelOrgCristinId()) || isNull(publication.getResourceOwner())) {
            return false;
        }

        return userInstance.getTopLevelOrgCristinId().equals(publication.getResourceOwner().getOwnerAffiliation());
    }

    protected boolean isProtectedDegreeInstanceTypeWithEmbargo() {
        return isProtectedDegreeInstanceType() && publication.getAssociatedArtifacts().stream()
                                                      .filter(File.class::isInstance)
                                                      .map(File.class::cast)
                                                      .anyMatch(this::hasEmbargo);
    }

    private boolean hasEmbargo(File file) {
        return !file.fileDoesNotHaveActiveEmbargo();
    }

    protected boolean isVerifiedContributor(Contributor contributor) {
        return contributor.getIdentity() != null && contributor.getIdentity().getId() != null;
    }

    protected boolean hasApprovedFiles() {
        return publication.getAssociatedArtifacts()
                   .stream()
                   .anyMatch(artifact -> ACCEPTED_FILE_TYPES
                                             .contains(artifact.getClass()));
    }

    protected boolean isOwner() {
        return nonNull(userInstance) && attempt(userInstance::getUsername)
                   .map(username -> UserInstance.fromPublication(publication).getUsername().equals(username))
                   .orElse(fail -> false);
    }

    protected boolean isDraft() {
        return publication.getStatus().equals(DRAFT);
    }

    protected boolean isUnpublished() {
        return publication.getStatus().equals(UNPUBLISHED);
    }

    protected boolean isPublished() {
        return publication.getStatus().equals(PUBLISHED);
    }

    private static Boolean publicationInstanceIsDegree(PublicationInstance<? extends Pages> publicationInstance) {
        return Arrays.stream(PROTECTED_DEGREE_INSTANCE_TYPES)
                   .anyMatch(instanceTypeClass -> instanceTypeClass.equals(publicationInstance.getClass()));
    }
}
