package no.unit.nva.publication.update;

import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public interface UpdateRequest {

    Resource generateUpdate(Resource resource) throws ForbiddenException;

    AssociatedArtifactList getAssociatedArtifacts();

    void authorize(PublicationPermissions permissions) throws UnauthorizedException;

    SortableIdentifier getIdentifier();
}
