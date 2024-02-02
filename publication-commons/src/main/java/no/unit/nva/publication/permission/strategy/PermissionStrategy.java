package no.unit.nva.publication.permission.strategy;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;

public abstract class PermissionStrategy {

    protected final Publication publication;
    protected final UserInstance userInstance;
    protected final List<AccessRight> accessRights;
    protected final URI personCristinId;

    protected PermissionStrategy(Publication publication, UserInstance userInstance, List<AccessRight> accessRights,
                              URI personCristinId) {
        this.publication = publication;
        this.userInstance = userInstance;
        this.accessRights = accessRights;
        this.personCristinId = personCristinId;
    }

    public abstract boolean hasPermissionToDelete();

    public abstract boolean hasPermissionToUnpublish();

    public abstract boolean hasPermissionToUpdate();

    protected boolean hasAccessRight(AccessRight accessRight) {
        return accessRights.contains(accessRight);
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
               || publicationInstance instanceof DegreePhd;
    }
}
