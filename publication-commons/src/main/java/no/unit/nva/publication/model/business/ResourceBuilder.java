package no.unit.nva.publication.model.business;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.ImportDetail;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationNoteBase;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.publication.model.business.importcandidate.ImportStatus;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import no.unit.nva.publication.model.business.publicationstate.ResourceEvent;

@SuppressWarnings({"PMD.TooManyFields", "PMD.CouplingBetweenObjects"})
public final class ResourceBuilder {

    private SortableIdentifier identifier;
    private PublicationStatus status;
    private Owner resourceOwner;
    private Organization publisher;
    private Instant createdDate;
    private Instant modifiedDate;
    private Instant publishedDate;
    private Instant indexedDate;
    private URI link;
    private AssociatedArtifactList associatedArtifacts;
    private List<FileEntry> fileEntries;
    private List<ResearchProject> projects;
    private EntityDescription entityDescription;
    private URI doi;
    private URI handle;
    private Set<AdditionalIdentifierBase> additionalIdentifiers;
    private List<URI> subjects;
    private List<Funding> fundings;
    private String rightsHolder;
    private ImportStatus importStatus;
    private List<PublicationNoteBase> publicationNotes;
    private URI duplicateOf;
    private Set<CuratingInstitution> curatingInstitutions;
    private List<ImportDetail> importDetails;
    private ResourceEvent resourceEvent;
    private List<PublicationChannel> publicationChannels;

    ResourceBuilder() {
    }

    public ResourceBuilder withIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
        return this;
    }

    public ResourceBuilder withStatus(PublicationStatus status) {
        this.status = status;
        return this;
    }

    public ResourceBuilder withResourceOwner(Owner resourceOwner) {
        this.resourceOwner = resourceOwner;
        return this;
    }

    public ResourceBuilder withPublisher(Organization publisher) {
        this.publisher = publisher;
        return this;
    }

    public ResourceBuilder withCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public ResourceBuilder withModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
        return this;
    }

    public ResourceBuilder withPublishedDate(Instant publishedDate) {
        this.publishedDate = publishedDate;
        return this;
    }

    public ResourceBuilder withIndexedDate(Instant indexedDate) {
        this.indexedDate = indexedDate;
        return this;
    }

    public ResourceBuilder withLink(URI link) {
        this.link = link;
        return this;
    }

    public ResourceBuilder withAssociatedArtifactsList(AssociatedArtifactList associatedArtifacts) {
        this.associatedArtifacts = new AssociatedArtifactList(associatedArtifacts);
        return this;
    }

    public ResourceBuilder withFilesEntries(List<FileEntry> fileEntries) {
        this.fileEntries = fileEntries;
        return this;
    }

    public ResourceBuilder withProjects(List<ResearchProject> projects) {
        this.projects = projects;
        return this;
    }

    public ResourceBuilder withEntityDescription(EntityDescription entityDescription) {
        this.entityDescription = entityDescription;
        return this;
    }

    public ResourceBuilder withDoi(URI doi) {
        this.doi = doi;
        return this;
    }

    public ResourceBuilder withHandle(URI handle) {
        this.handle = handle;
        return this;
    }

    public ResourceBuilder withAdditionalIdentifiers(Set<AdditionalIdentifierBase> additionalIdentifiers) {
        this.additionalIdentifiers = additionalIdentifiers;
        return this;
    }

    public ResourceBuilder withSubjects(List<URI> subjects) {
        this.subjects = subjects;
        return this;
    }

    public ResourceBuilder withFundings(List<Funding> fundings) {
        this.fundings = fundings;
        return this;
    }

    public ResourceBuilder withRightsHolder(String rightsHolder) {
        this.rightsHolder = rightsHolder;
        return this;
    }

    public ResourceBuilder withImportStatus(ImportStatus importStatus) {
        this.importStatus = importStatus;
        return this;
    }

    public ResourceBuilder withPublicationNotes(List<PublicationNoteBase> publicationNotes) {
        this.publicationNotes = publicationNotes;
        return this;
    }

    public ResourceBuilder withDuplicateOf(URI duplicateOf) {
        this.duplicateOf = duplicateOf;
        return this;
    }

    public ResourceBuilder withCuratingInstitutions(Set<CuratingInstitution> curatingInstitutions) {
        this.curatingInstitutions = curatingInstitutions;
        return this;
    }

    public ResourceBuilder withPublicationChannels(List<PublicationChannel> publicationChannels) {
        this.publicationChannels = publicationChannels;
        return this;
    }

    public ResourceBuilder withImportDetails(Collection<ImportDetail> importDetails) {
        this.importDetails = new ArrayList<>(importDetails);
        return this;
    }

    public ResourceBuilder withResourceEvent(ResourceEvent resourceEvent) {
        this.resourceEvent = resourceEvent;
        return this;
    }

    public Resource build() {
        Resource resource = new Resource();
        resource.setIdentifier(identifier);
        resource.setStatus(status);
        resource.setResourceOwner(resourceOwner);
        resource.setPublisher(publisher);
        resource.setCreatedDate(createdDate);
        resource.setModifiedDate(modifiedDate);
        resource.setPublishedDate(publishedDate);
        resource.setIndexedDate(indexedDate);
        resource.setLink(link);
        resource.setAssociatedArtifacts(associatedArtifacts);
        resource.setFileEntries(fileEntries);
        resource.setProjects(projects);
        resource.setEntityDescription(entityDescription);
        resource.setDoi(doi);
        resource.setHandle(handle);
        resource.setAdditionalIdentifiers(additionalIdentifiers);
        resource.setSubjects(subjects);
        resource.setFundings(fundings);
        resource.setRightsHolder(rightsHolder);
        resource.setImportStatus(importStatus);
        resource.setPublicationNotes(publicationNotes);
        resource.setDuplicateOf(duplicateOf);
        resource.setCuratingInstitutions(curatingInstitutions);
        resource.setImportDetails(importDetails);
        resource.setResourceEvent(resourceEvent);
        resource.setPublicationChannels(publicationChannels);
        return resource;
    }
}
