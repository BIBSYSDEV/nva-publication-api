package no.unit.nva;

import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;

public interface WithAssociatedArtifact extends PublicationBase {

    AssociatedArtifactList getAssociatedArtifacts();

    void setAssociatedArtifacts(AssociatedArtifactList associatedArtifact);

}
