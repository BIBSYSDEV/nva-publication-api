package no.unit.nva.publication.update;

import java.util.List;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.funding.Funding;

public record PartialUpdatePublicationRequest(List<Funding> fundings, List<ResearchProject> projects,
                                              AssociatedArtifactList associatedArtifacts)
    implements PublicationRequest {

}
