package no.unit.nva.publication.permission.strategy;

import java.util.Optional;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;

public abstract class PermissionStrategy {

    protected final Publication publication;
    protected final UserInstance userInstance;

    protected PermissionStrategy(Publication publication, UserInstance userInstance) {
        this.publication = publication;
        this.userInstance = userInstance;
    }

    protected abstract boolean allowsAction(PublicationAction permission);

    protected boolean hasAccessRight(AccessRight accessRight) {
        return userInstance.getAccessRights().contains(accessRight);
    }

    protected boolean isDegree() {
        return Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .map(PermissionStrategy::publicationInstanceIsDegree)
                   .orElse(false);
    }

    private static Boolean publicationInstanceIsDegree(PublicationInstance<? extends Pages> publicationInstance) {
        return publicationInstance instanceof DegreeBachelor
               || publicationInstance instanceof DegreeMaster
               || publicationInstance instanceof DegreePhd
               || publicationInstance instanceof DegreeLicentiate;
    }
}
