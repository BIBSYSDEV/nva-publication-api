package no.unit.nva.publication.update;

import static no.unit.nva.publication.update.UpdatePublicationRequest.WRONG_PUBLICATION_UPDATE_ERROR;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

@JsonTypeName("PartialUpdatePublicationRequest")
public final class PartialUpdatePublicationRequest implements PublicationRequest, UpdateRequest {

    private final SortableIdentifier identifier;
    private final Set<Funding> fundings;
    private final List<ResearchProject> projects;
    private final AssociatedArtifactList associatedArtifacts;

    @JsonCreator
    public PartialUpdatePublicationRequest(@JsonProperty("identifier") SortableIdentifier identifier,
                                           @JsonProperty("fundings") Set<Funding> fundings,
                                           @JsonProperty("projects") List<ResearchProject> projects,
                                           @JsonProperty("associatedArtifacts") AssociatedArtifactList associatedArtifacts) {
        this.identifier = identifier;
        this.fundings = fundings;
        this.projects = projects;
        this.associatedArtifacts = associatedArtifacts;
    }

    public Set<Funding> getFundings() {
        return fundings;
    }

    public List<ResearchProject> getProjects() {
        return projects;
    }

    @Override
    public Resource generateUpdate(Resource resource) {
        if (!getIdentifier().equals(resource.getIdentifier())) {
            throw new IllegalArgumentException(WRONG_PUBLICATION_UPDATE_ERROR + resource.getIdentifier());
        }
        return resource.copy()
                   .withAssociatedArtifactsList(getAssociatedArtifacts())
                   .withFundings(getFundings())
                   .withProjects(getProjects())
                   .build();
    }

    @Override
    public AssociatedArtifactList getAssociatedArtifacts() {
        return associatedArtifacts;
    }

    @Override
    public void authorize(PublicationPermissions permissions) throws UnauthorizedException {
        permissions.authorize(PublicationOperation.PARTIAL_UPDATE);
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(identifier, fundings, projects, associatedArtifacts);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (PartialUpdatePublicationRequest) obj;
        return Objects.equals(this.identifier, that.identifier) && Objects.equals(this.fundings, that.fundings) &&
               Objects.equals(this.projects, that.projects) &&
               Objects.equals(this.associatedArtifacts, that.associatedArtifacts);
    }
}
