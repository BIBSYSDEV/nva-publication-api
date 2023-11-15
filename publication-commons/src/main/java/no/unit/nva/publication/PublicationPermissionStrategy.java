package no.unit.nva.publication;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.role.Role.CREATOR;
import static nva.commons.apigateway.AccessRight.EDIT_ALL_NON_DEGREE_RESOURCES;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_RESOURCES;
import static nva.commons.apigateway.AccessRight.PUBLISH_DEGREE;
import static nva.commons.core.attempt.Try.attempt;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;

public class PublicationPermissionStrategy {
    private final Set<PermissionStrategy> permissionStrategies;

    public PublicationPermissionStrategy() {
        this.permissionStrategies = new HashSet<>();
        this.permissionStrategies.add(new EditorPermissionStrategy());
        this.permissionStrategies.add(new CuratorPermissionStrategy());
        this.permissionStrategies.add(new ContributorPermissionStrategy());
        this.permissionStrategies.add(new ResourceOwnerPermissionStrategy());
    }

    public boolean hasPermissionToUnpublish(RequestInfo requestInfo, Publication publication) {
        for (PermissionStrategy strategy : permissionStrategies) {
            if (strategy.hasPermission(requestInfo, publication)) {
                return true;
            }
        }
        return false;
    }
}

abstract class PermissionStrategy {

    protected PermissionStrategy() {}

    public abstract boolean hasPermission(RequestInfo requestInfo, Publication resource);

    public static boolean hasAccessRight(RequestInfo requestInfo, AccessRight accessRight) {
        return requestInfo.userIsAuthorized(accessRight.name());
    }
}

class EditorPermissionStrategy extends PermissionStrategy {

    public EditorPermissionStrategy() {
        super();
    }

    @Override
    public boolean hasPermission(RequestInfo requestInfo, Publication publication) {
        if (isDegree(publication)) {
            return hasAccessRight(requestInfo, PUBLISH_DEGREE);
        }

        return hasAccessRight(requestInfo, EDIT_ALL_NON_DEGREE_RESOURCES);
    }

    private static boolean isDegree(Publication publication) {
        return Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .map(EditorPermissionStrategy::publicationInstanceIsDegree)
                   .orElse(false);
    }

    private static Boolean publicationInstanceIsDegree(PublicationInstance<? extends Pages> publicationInstance) {
        return publicationInstance instanceof DegreeBachelor
               || publicationInstance instanceof DegreeMaster
               || publicationInstance instanceof DegreePhd;
    }
}

class CuratorPermissionStrategy extends PermissionStrategy {

    @Override
    public boolean hasPermission(RequestInfo requestInfo, Publication publication) {
        if (!userIsFromSameInstitutionAsPublication(requestInfo, publication)) {
            return false;
        }

        return hasAccessRight(requestInfo, EDIT_OWN_INSTITUTION_RESOURCES);
    }

    private static Boolean userIsFromSameInstitutionAsPublication(RequestInfo requestInfo, Publication publication) {
        var requestInfoCurrentCustomer = attempt(requestInfo::getCurrentCustomer)
                                             .orElse(uriFailure -> null);

        if (requestInfoCurrentCustomer == null) {
            return false;
        }

        return publication.getPublisher().getId().equals(requestInfoCurrentCustomer);
    }
}

class ContributorPermissionStrategy extends PermissionStrategy {

    @Override
    public boolean hasPermission(RequestInfo requestInfo, Publication publication) {
        var identity = attempt(requestInfo::getPersonCristinId)
                           .orElse(uriFailure -> null);

        if (identity == null) {
            return false;
        }

        return publication.getEntityDescription()
                   .getContributors()
                   .stream()
                   .filter(ContributorPermissionStrategy::isCreator)
                   .map(Contributor::getIdentity)
                   .map(Identity::getId)
                   .filter(Objects::nonNull)
                   .anyMatch(identity::equals);
    }

    private static boolean isCreator(Contributor contributor) {
        return nonNull(contributor.getRole()) && CREATOR.equals(contributor.getRole().getType());
    }
}

class ResourceOwnerPermissionStrategy extends PermissionStrategy {

    @Override
    public boolean hasPermission(RequestInfo requestInfo, Publication publication) {
        var username = attempt(requestInfo::getUserName)
                           .orElse(stringFailure -> null);

        if (username == null) {
            return false;
        }

        return UserInstance.fromPublication(publication).getUsername().equals(username);
    }
}