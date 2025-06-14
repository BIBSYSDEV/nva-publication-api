package no.unit.nva.publication.permissions.publication;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.model.associatedartifacts.file.File.APPROVED_FILE_TYPES;
import static no.unit.nva.model.associatedartifacts.file.File.FINALIZED_FILE_TYPES;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Reference;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicationStrategyBase {

    public static final Logger logger = LoggerFactory.getLogger(PublicationStrategyBase.class);
    private static final Set<String> IMPORT_IDENTIFIER_SOURCES = Set.of("inspera", "wiseflow");

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

    protected boolean userRelatesToPublication() {
        return userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution()
               || userBelongsToPublicationChannelOwner();
    }

    protected boolean userBelongsToPublicationChannelOwner() {
        if (Optional.ofNullable(userInstance).map(UserInstance::getTopLevelOrgCristinId).isEmpty()) {
            return false;
        }

        var claimedPublicationChannel = resource.getPrioritizedClaimedPublicationChannelWithinScope();

        if (claimedPublicationChannel.isEmpty()) {
            return false;
        }

        var channelOwner = claimedPublicationChannel.get().getOrganizationId();
        return userInstance.getTopLevelOrgCristinId().equals(channelOwner);
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
        if (isNull(userInstance) || isNull(userInstance.getTopLevelOrgCristinId()) || isNull(
            resource.getResourceOwner())) {
            return false;
        }

        return userInstance.getTopLevelOrgCristinId().equals(resource.getResourceOwner().getOwnerAffiliation());
    }

    protected boolean isVerifiedContributor(Contributor contributor) {
        return Optional.ofNullable(contributor.getIdentity()).map(Identity::getId).isPresent();
    }

    protected boolean hasApprovedFiles() {
        return resource.getAssociatedArtifacts()
                   .stream()
                   .anyMatch(artifact -> APPROVED_FILE_TYPES.contains(artifact.getClass()));
    }

    protected boolean hasFinalizedFiles() {
        return resource.getAssociatedArtifacts()
                   .stream()
                   .anyMatch(artifact -> FINALIZED_FILE_TYPES.contains(artifact.getClass()));
    }

    protected boolean isImportedStudentThesis() {
        return resource.getAdditionalIdentifiers().stream()
                   .filter(AdditionalIdentifier.class::isInstance)
                   .anyMatch(
                       identifier -> IMPORT_IDENTIFIER_SOURCES.contains(
                           identifier.sourceName().toLowerCase(Locale.ROOT)));
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
