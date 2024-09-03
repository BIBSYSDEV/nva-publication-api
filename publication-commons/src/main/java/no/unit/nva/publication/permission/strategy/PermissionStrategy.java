package no.unit.nva.publication.permission.strategy;

import static java.util.Objects.isNull;
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
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.AccessRight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PermissionStrategy {

    public static final Logger logger = LoggerFactory.getLogger(PermissionStrategy.class);

    public static final Class<?>[] PROTECTED_DEGREE_INSTANCE_TYPES = {
        DegreeLicentiate.class,
        DegreeBachelor.class,
        DegreeMaster.class,
        DegreePhd.class
    };

    protected final Publication publication;
    protected final UserInstance userInstance;
    protected final ResourceService resourceService;

    protected PermissionStrategy(Publication publication, UserInstance userInstance, ResourceService resourceService) {
        this.publication = publication;
        this.userInstance = userInstance;
        this.resourceService = resourceService;
    }

    protected boolean hasAccessRight(AccessRight accessRight) {
        return userInstance.getAccessRights().contains(accessRight);
    }

    protected boolean isProtectedDegreeInstanceType() {
        return Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .map(PermissionStrategy::publicationInstanceIsDegree)
                   .orElse(false);
    }

    protected boolean userSharesTopLevelOrgWithAtLeastOneContributor() {
        var userTopLevelOrg = userInstance.getTopLevelOrgCristinId();

        logger.info("found topLevels {} for user {} of {}.",
                    publication.getCuratingInstitutions(),
                    userInstance.getUser(),
                    userTopLevelOrg);

        return publication.getCuratingInstitutions().stream().anyMatch(org -> org.equals(userTopLevelOrg));
    }

    protected boolean userIsFromSameInstitutionAsPublication() {
        if (isNull(userInstance.getTopLevelOrgCristinId()) || isNull(publication.getResourceOwner())) {
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

    protected boolean hasNoPendingTicket(Class<? extends TicketEntry> ticketType) {
        var resource = Resource.fromPublication(publication);
        return resourceService.fetchAllTicketsForResource(resource)
                   .filter(ticketType::isInstance)
                   .noneMatch(TicketEntry::isPending);
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

    protected boolean hasUnpublishedFile() {
        return publication.getAssociatedArtifacts().stream().anyMatch(UnpublishedFile.class::isInstance);
    }

    protected Boolean isOwner() {
        return attempt(userInstance::getUsername)
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
