package no.unit.nva.publication.permission.strategy;

import java.util.Optional;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.pages.Pages;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;

public abstract class PermissionStrategy {

    public abstract boolean hasPermissionToDelete(RequestInfo requestInfo, Publication publication);

    public abstract boolean hasPermissionToUnpublish(RequestInfo requestInfo, Publication publication);

    public abstract boolean hasPermissionToUpdate(RequestInfo requestInfo, Publication publication);

    protected static boolean hasAccessRight(RequestInfo requestInfo, AccessRight accessRight) {
        return requestInfo.userIsAuthorized(accessRight);
    }

    protected static boolean isDegree(Publication publication) {
        return Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .map(PermissionStrategy::publicationInstanceIsDegree)
                   .orElse(false);
    }

    private static Boolean publicationInstanceIsDegree(PublicationInstance<? extends Pages> publicationInstance) {
        return publicationInstance instanceof DegreeBachelor
               || publicationInstance instanceof DegreeMaster
               || publicationInstance instanceof DegreePhd;
    }
}
