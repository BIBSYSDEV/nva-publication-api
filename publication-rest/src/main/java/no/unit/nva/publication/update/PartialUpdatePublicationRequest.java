package no.unit.nva.publication.update;

import static no.unit.nva.publication.update.UpdatePublicationRequest.WRONG_PUBLICATION_UPDATE_ERROR;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.publication.model.business.Resource;

public record PartialUpdatePublicationRequest(SortableIdentifier identifier, List<Funding> fundings,
                                              List<ResearchProject> projects,
                                              AssociatedArtifactList associatedArtifacts)
    implements PublicationRequest {

    public Resource generateUpdate(Resource resource) {
        if (!identifier().equals(resource.getIdentifier())) {
            throw new IllegalArgumentException(
                WRONG_PUBLICATION_UPDATE_ERROR + resource.getIdentifier());
        }
        return resource.copy()
                   .withAssociatedArtifactsList(associatedArtifacts())
                   .withFundings(fundings())
                   .withProjects(projects())
                   .build();
    }
}
