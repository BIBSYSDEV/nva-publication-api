package no.unit.nva.publication.update;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.WithContext;
import no.unit.nva.WithIdentifier;
import no.unit.nva.WithMetadata;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.ImportDetail;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

@JsonTypeName("Publication")
public class UpdatePublicationRequest
    implements PublicationRequest, WithIdentifier, WithMetadata, WithContext, UpdateRequest {

    public static final String WRONG_PUBLICATION_UPDATE_ERROR = "Trying to update a publication with different "
                                                                + "identifier:";
    private SortableIdentifier identifier;
    private EntityDescription entityDescription;
    private AssociatedArtifactList associatedArtifacts;
    @JsonProperty("@context")
    private JsonNode context;
    private List<ResearchProject> projects;
    private List<URI> subjects;
    private Set<Funding> fundings;
    private String rightsHolder;
    private List<ImportDetail> importDetails;
    private Set<AdditionalIdentifierBase> additionalIdentifiers;

    @Override
    public Resource generateUpdate(Resource resource) throws ForbiddenException {
        if (!this.identifier.equals(resource.getIdentifier())) {
            throw new IllegalArgumentException(
                WRONG_PUBLICATION_UPDATE_ERROR + resource.getIdentifier());
        }
        return validateNonNulls(resource.copy()
                                    .withEntityDescription(this.entityDescription)
                                    .withAssociatedArtifactsList(this.associatedArtifacts)
                                    .withProjects(this.projects)
                                    .withSubjects(this.subjects)
                                    .withFundings(this.fundings)
                                    .withRightsHolder(this.rightsHolder)
                                    .withAdditionalIdentifiers(
                                        mergeAdditionalIdentifiers(resource.getAdditionalIdentifiers()))
                                    .build());
    }

    private Resource validateNonNulls(Resource resource)
        throws ForbiddenException {
        if (isNull(resource.getEntityDescription())) {
            throw new ForbiddenException();
        }
        return resource;
    }

    @Override
    public void authorize(PublicationPermissions permissions, Resource existingResource)
        throws UnauthorizedException {
        permissions.authorize(PublicationOperation.UPDATE);
        if (hasNewAdditionalIdentifiers(existingResource)) {
            permissions.authorize(PublicationOperation.ADD_ADDITIONAL_IDENTIFIERS);
        }
    }

    @JacocoGenerated
    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @JacocoGenerated
    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    @JacocoGenerated
    @Override
    public EntityDescription getEntityDescription() {
        return entityDescription;
    }

    @JacocoGenerated
    @Override
    public void setEntityDescription(EntityDescription entityDescription) {
        this.entityDescription = entityDescription;
    }

    @Override
    @JacocoGenerated
    public List<ResearchProject> getProjects() {
        return projects;
    }

    @Override
    @JacocoGenerated
    public void setProjects(List<ResearchProject> projects) {
        this.projects = projects;
    }

    @Override
    @JacocoGenerated
    public List<URI> getSubjects() {
        return subjects;
    }

    @Override
    @JacocoGenerated
    public void setSubjects(List<URI> subjects) {
        this.subjects = subjects;
    }

    @Override
    @JacocoGenerated
    public AssociatedArtifactList getAssociatedArtifacts() {
        return associatedArtifacts;
    }

    @JacocoGenerated
    public void setAssociatedArtifacts(AssociatedArtifactList associatedArtifacts) {
        this.associatedArtifacts = associatedArtifacts;
    }

    @Override
    @JacocoGenerated
    public JsonNode getContext() {
        return context;
    }

    @Override
    @JacocoGenerated
    public void setContext(JsonNode context) {
        this.context = context;
    }

    @Override
    @JacocoGenerated
    public Set<Funding> getFundings() {
        return fundings;
    }

    @Override
    @JacocoGenerated
    public void setFundings(Set<Funding> fundings) {
        this.fundings = fundings;
    }

    @Override
    @JacocoGenerated
    public String getRightsHolder() {
        return rightsHolder;
    }

    @Override
    @JacocoGenerated
    public void setRightsHolder(String rightsHolder) {
        this.rightsHolder = rightsHolder;
    }

    @Override
    @JacocoGenerated
    public List<ImportDetail> getImportDetails() {
        return nonNull(importDetails) ? importDetails : Collections.emptyList();
    }

    @Override
    @JacocoGenerated
    public void setImportDetails(Collection<ImportDetail> importDetails) {
        this.importDetails = new ArrayList<>(importDetails);
    }

    @JacocoGenerated
    public Set<AdditionalIdentifierBase> getAdditionalIdentifiers() {
        return additionalIdentifiers;
    }

    @JacocoGenerated
    public void setAdditionalIdentifiers(Set<AdditionalIdentifierBase> additionalIdentifiers) {
        this.additionalIdentifiers = additionalIdentifiers;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(identifier, entityDescription, associatedArtifacts, subjects, context, importDetails);
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UpdatePublicationRequest that = (UpdatePublicationRequest) o;
        return Objects.equals(identifier, that.identifier)
               && Objects.equals(entityDescription, that.entityDescription)
               && Objects.equals(associatedArtifacts, that.associatedArtifacts)
               && Objects.equals(subjects, that.subjects)
               && Objects.equals(context, that.context)
               && Objects.equals(importDetails, that.importDetails);
    }

    private boolean hasAdditionalIdentifiers() {
        return nonNull(additionalIdentifiers) && !additionalIdentifiers.isEmpty();
    }

    private boolean hasNewAdditionalIdentifiers(Resource existingResource) {
        return hasAdditionalIdentifiers()
               && !existingResource.getAdditionalIdentifiers().containsAll(additionalIdentifiers);
    }

    private Set<AdditionalIdentifierBase> mergeAdditionalIdentifiers(
        Set<AdditionalIdentifierBase> existingIdentifiers) {
        if (!hasAdditionalIdentifiers()) {
            return existingIdentifiers;
        }
        var merged = new HashSet<>(existingIdentifiers);
        merged.addAll(additionalIdentifiers);
        return merged;
    }
}

