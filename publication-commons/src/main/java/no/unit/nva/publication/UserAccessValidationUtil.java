package no.unit.nva.publication;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.role.Role.CREATOR;
import static nva.commons.apigateway.AccessRight.EDIT_ALL_NON_DEGREE_RESOURCES;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_RESOURCES;
import static nva.commons.apigateway.AccessRight.PUBLISH_DEGREE;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public class UserAccessValidationUtil {

    public static void validateUserAccessForDeletingPublishedPublication(RequestInfo requestInfo,
                                                                        Publication publication)
        throws UnauthorizedException {

        if (!PublicationStatus.PUBLISHED.equals(publication.getStatus())) {
            return;
        }

        if (isAuthorizedToUnpublishPublication(requestInfo, publication)) {
            return;
        }

        throw new UnauthorizedException();
    }

    private static boolean isAuthorizedToUnpublishPublication(RequestInfo requestInfo, Publication publication)
        throws UnauthorizedException {

        return isEditor(publication, requestInfo)
               || canEditResourcesForInstitution(publication, requestInfo)
               || isPublicationOwner(publication, requestInfo.getUserName())
               || isContributor(publication, requestInfo.getPersonCristinId());
    }

    private static boolean isEditor(Publication publication, RequestInfo requestInfo) {
        if (isDegree(publication)) {
            return hasAccessRight(requestInfo, PUBLISH_DEGREE);
        }

        return hasAccessRight(requestInfo, EDIT_ALL_NON_DEGREE_RESOURCES);
    }

    private static boolean isDegree(Publication publication) {
        return Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .map(UserAccessValidationUtil::publicationInstanceIsDegree)
                   .orElse(false);
    }

    private static Boolean publicationInstanceIsDegree(PublicationInstance<? extends Pages> publicationInstance) {
        return publicationInstance instanceof DegreeBachelor
               || publicationInstance instanceof DegreeMaster
               || publicationInstance instanceof DegreePhd;
    }

    private static boolean canEditResourcesForInstitution(Publication publication, RequestInfo requestInfo)
        throws UnauthorizedException {

        if (!userIsFromSameInstitutionAsPublication(requestInfo, publication)) {
            return false;
        }

        return hasAccessRight(requestInfo, EDIT_OWN_INSTITUTION_RESOURCES);
    }

    private static Boolean userIsFromSameInstitutionAsPublication(RequestInfo requestInfo, Publication publication)
        throws UnauthorizedException {

        var requestInfoCurrentCustomer = requestInfo.getCurrentCustomer();
        return publication.getPublisher().getId().equals(requestInfoCurrentCustomer);
    }

    private static boolean isPublicationOwner(Publication publication, String username) {
        return UserInstance.fromPublication(publication).getUsername().equals(username);
    }

    private static boolean isContributor(Publication publication, URI identity) {
        return publication.getEntityDescription()
                   .getContributors()
                   .stream()
                   .filter(UserAccessValidationUtil::isCreator)
                   .map(Contributor::getIdentity)
                   .map(Identity::getId)
                   .filter(Objects::nonNull)
                   .anyMatch(identity::equals);
    }

    private static boolean isCreator(Contributor contributor) {
        return nonNull(contributor.getRole()) && CREATOR.equals(contributor.getRole().getType());
    }

    private static boolean hasAccessRight(RequestInfo requestInfo, AccessRight accessRight) {
        return requestInfo.userIsAuthorized(accessRight.name());
    }
}
