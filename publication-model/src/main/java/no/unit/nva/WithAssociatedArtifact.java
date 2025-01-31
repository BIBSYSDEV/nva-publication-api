package no.unit.nva;

import java.util.List;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactResponse;

public interface WithAssociatedArtifact extends PublicationBase {

    List<AssociatedArtifactResponse> getAssociatedArtifacts();

    void setAssociatedArtifacts(List<AssociatedArtifactResponse> associatedArtifact);

}
