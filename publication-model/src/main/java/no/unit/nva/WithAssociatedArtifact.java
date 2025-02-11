package no.unit.nva;

import java.util.List;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactDto;

public interface WithAssociatedArtifact extends PublicationBase {

    List<AssociatedArtifactDto> getAssociatedArtifacts();

    void setAssociatedArtifacts(List<AssociatedArtifactDto> associatedArtifact);

}
