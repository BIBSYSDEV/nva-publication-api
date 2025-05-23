package no.unit.nva.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationNoteBase;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactDto;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings({"PMD.TooManyFields", "PMD.GodClass"})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("Publication")
public class PublicationResponseElevatedUser extends PublicationResponse {

    private List<PublicationNoteBase> publicationNotes;
    private Set<CuratingInstitution> curatingInstitutions;

    public static PublicationResponseElevatedUser fromPublication(Publication publication) {
        var response = new PublicationResponseElevatedUser();
        response.setIdentifier(publication.getIdentifier());
        response.setDuplicateOf(publication.getDuplicateOf());
        response.setStatus(publication.getStatus());
        response.setResourceOwner(publication.getResourceOwner());
        response.setPublisher(publication.getPublisher());
        response.setCreatedDate(publication.getCreatedDate());
        response.setModifiedDate(publication.getModifiedDate());
        response.setPublishedDate(publication.getPublishedDate());
        response.setIndexedDate(publication.getIndexedDate());
        response.setHandle(publication.getHandle());
        response.setLink(publication.getLink());
        response.setEntityDescription(publication.getEntityDescription());
        response.setDoi(publication.getDoi());
        response.setProjects(publication.getProjects());
        response.setFundings(publication.getFundings());
        response.setSubjects(publication.getSubjects());
        response.setContext(PublicationContext.getContext(publication));
        response.setAssociatedArtifacts(publication.getAssociatedArtifacts().stream()
            .map(AssociatedArtifact::toDto)
            .toList());
        response.setAdditionalIdentifiers(publication.getAdditionalIdentifiers());
        response.setRightsHolder(publication.getRightsHolder());
        response.setPublicationNotes(publication.getPublicationNotes());
        response.setAllowedOperations(Set.of());
        response.setImportDetails(publication.getImportDetails());
        response.setPendingOpenFileCount(publication.getPendingOpenFileCount());
        response.setCuratingInstitutions(publication.getCuratingInstitutions());
        return response;
    }

    public static PublicationResponseElevatedUser fromPublicationWithAllowedOperations(
        Publication publication,
        Set<PublicationOperation> allowedOperations
    ) {
        var response = fromPublication(publication);
        response.setAllowedOperations(allowedOperations);
        return response;
    }

    public List<PublicationNoteBase> getPublicationNotes() {
        return publicationNotes;
    }

    public void setPublicationNotes(List<PublicationNoteBase> publicationNotes) {
        this.publicationNotes = publicationNotes;
    }

    public Set<CuratingInstitution> getCuratingInstitutions() {
        return curatingInstitutions;
    }

    public void setCuratingInstitutions(Set<CuratingInstitution> curatingInstitutions) {
        this.curatingInstitutions = curatingInstitutions;
    }

    @Override
    public List<AssociatedArtifactDto> getAssociatedArtifacts() {
        return super.getAssociatedArtifactsForElevatedUser();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), publicationNotes, curatingInstitutions);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        PublicationResponseElevatedUser that = (PublicationResponseElevatedUser) o;
        return Objects.equals(publicationNotes, that.publicationNotes)
               && Objects.equals(curatingInstitutions, that.curatingInstitutions);
    }
}
