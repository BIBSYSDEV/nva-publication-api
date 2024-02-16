package no.unit.nva.publication.permission.strategy.restrict;

import java.util.Optional;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.PermissionStrategy;
import nva.commons.apigateway.AccessRight;

public abstract class RestrictPermissionStrategy extends PermissionStrategy {

    protected RestrictPermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    public abstract boolean deniesAction(PublicationOperation permission);
}
