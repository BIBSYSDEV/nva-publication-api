package no.unit.nva.publication.model.business;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationStatus.DELETED;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.ImportDetail;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationNoteBase;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.funding.FundingList;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatus;
import no.unit.nva.publication.model.business.logentry.LogEntry;
import no.unit.nva.publication.model.business.publicationstate.DeletedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.ImportedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.PublishedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.RepublishedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.ResourceEvent;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings({"PMD.GodClass", "PMD.TooManyFields", "PMD.ExcessivePublicCount"})
@JsonTypeInfo(use = Id.NAME, property = "type")
public class Resource implements Entity {

    public static final String TYPE = "Resource";
    public static final URI NOT_IMPORTANT = null;
    public static final List<PublicationStatus> PUBLISHABLE_STATUSES = List.of(DRAFT, PUBLISHED_METADATA,
                                                                               UNPUBLISHED);

    @JsonProperty
    private SortableIdentifier identifier;
    @JsonProperty
    private PublicationStatus status;
    @JsonProperty
    private Owner resourceOwner;
    @JsonProperty
    private Organization publisher;
    @JsonProperty
    private Instant createdDate;
    @JsonProperty
    private Instant modifiedDate;
    @JsonProperty
    private Instant publishedDate;
    @JsonProperty
    private Instant indexedDate;
    @JsonProperty
    private URI link;
    @JsonProperty
    private AssociatedArtifactList associatedArtifacts;
    @JsonProperty
    private List<ResearchProject> projects;
    @JsonProperty
    private EntityDescription entityDescription;
    @JsonProperty
    private URI doi;
    @JsonProperty
    private URI handle;
    @JsonProperty
    private Set<AdditionalIdentifierBase> additionalIdentifiers;
    @JsonProperty
    private List<URI> subjects;
    @JsonProperty
    private List<Funding> fundings;
    @JsonProperty
    private String rightsHolder;
    @JsonProperty
    private ImportStatus importStatus;
    @JsonProperty
    private List<PublicationNoteBase> publicationNotes;
    @JsonProperty
    private URI duplicateOf;
    @JsonProperty
    private Set<CuratingInstitution> curatingInstitutions;
    @JsonProperty
    private List<ImportDetail> importDetails;
    @JsonProperty
    private ResourceEvent resourceEvent;
    @JsonIgnore
    private List<FileEntry> files;

    public static Resource resourceQueryObject(UserInstance userInstance, SortableIdentifier resourceIdentifier) {
        return emptyResource(userInstance.getUser(), userInstance.getCustomerId(),
                             resourceIdentifier);
    }

    public static Resource resourceQueryObject(SortableIdentifier resourceIdentifier) {
        Resource resource = new Resource();
        resource.setIdentifier(resourceIdentifier);
        return resource;
    }

    public static Resource emptyResource(User username,
                                         URI organizationId,
                                         SortableIdentifier resourceIdentifier) {
        Resource resource = new Resource();
        resource.setPublisher(new Organization.Builder().withId(organizationId).build());
        resource.setResourceOwner(new Owner(username, NOT_IMPORTANT));
        resource.setIdentifier(resourceIdentifier);
        return resource;
    }

    public static Resource fromPublication(Publication publication) {
        return Optional.ofNullable(publication).map(Resource::convertToResource).orElse(null);
    }

    public ResourceEvent getResourceEvent() {
        return resourceEvent;
    }

    public void setResourceEvent(ResourceEvent resourceEvent) {
        this.resourceEvent = resourceEvent;
    }

    public boolean hasResourceEvent() {
        return nonNull(getResourceEvent());
    }

    @JsonIgnore
    public List<File> getFiles() {
        return getAssociatedArtifacts().stream()
                   .filter(File.class::isInstance)
                   .map(File.class::cast)
                   .toList();
    }

    @JsonIgnore
    public List<FileEntry> getFileEntries() {
        return nonNull(files) ? files : Collections.emptyList();
    }

    public Optional<FileEntry> getFileEntry(SortableIdentifier identifier) {
        return getFileEntries().stream().filter(file -> file.getIdentifier().equals(identifier)).findFirst();
    }

    public void setFileEntries(List<FileEntry> files) {
        this.files = files;
    }

    public PublicationSummary toSummary() {
        return PublicationSummary.create(this.toPublication());
    }

    public Resource delete(UserInstance userInstance, Instant currentTime) {
        return new ResourceBuilder()
                   .withIdentifier(getIdentifier())
                   .withStatus(DELETED)
                   .withDoi(getDoi())
                   .withPublisher(getPublisher())
                   .withResourceOwner(getResourceOwner())
                   .withEntityDescription(getEntityDescription())
                   .withCreatedDate(getCreatedDate())
                   .withPublishedDate(getPublishedDate())
                   .withModifiedDate(currentTime)
                   .withResourceEvent(DeletedResourceEvent.create(userInstance, currentTime))
                   .build();
    }

    private static Resource convertToResource(Publication publication) {
        return Resource.builder()
                   .withIdentifier(publication.getIdentifier())
                   .withResourceOwner(Owner.fromResourceOwner(publication.getResourceOwner()))
                   .withCreatedDate(publication.getCreatedDate())
                   .withModifiedDate(publication.getModifiedDate())
                   .withIndexedDate(publication.getIndexedDate())
                   .withPublishedDate(publication.getPublishedDate())
                   .withStatus(publication.getStatus())
                   .withAssociatedArtifactsList(publication.getAssociatedArtifacts())
                   .withFilesEntries(getFileEntriesFromPublication(publication))
                   .withPublisher(publication.getPublisher())
                   .withLink(publication.getLink())
                   .withProjects(publication.getProjects())
                   .withEntityDescription(publication.getEntityDescription())
                   .withDoi(publication.getDoi())
                   .withHandle(publication.getHandle())
                   .withAdditionalIdentifiers(publication.getAdditionalIdentifiers())
                   .withSubjects(publication.getSubjects())
                   .withFundings(publication.getFundings())
                   .withRightsHolder(publication.getRightsHolder())
                   .withPublicationNotes(publication.getPublicationNotes())
                   .withDuplicateOf(publication.getDuplicateOf())
                   .withCuratingInstitutions(publication.getCuratingInstitutions())
                   .withImportDetails(publication.getImportDetails())
                   .build();
    }

    /**
     * Extracts FileEntries from a Publication.
     *
     * <p><b style="color: red;">Warning:</b> This method does not include all the needed FileEntry meta data and
     * should not be used when handling files.</p>
     *
     * @param publication the Publication extract FileEntries from.
     * @return the list of FileEntries.
     */
    private static List<FileEntry> getFileEntriesFromPublication(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .filter(File.class::isInstance)
                   .map(File.class::cast)
                   .map(file -> FileEntry.create(file, publication.getIdentifier(),
                                                 UserInstance.fromPublication(publication)))
                   .toList();
    }

    private static Resource convertToResource(ImportCandidate importCandidate) {
        return Resource.builder()
                   .withIdentifier(importCandidate.getIdentifier())
                   .withResourceOwner(Owner.fromResourceOwner(importCandidate.getResourceOwner()))
                   .withCreatedDate(importCandidate.getCreatedDate())
                   .withModifiedDate(importCandidate.getModifiedDate())
                   .withIndexedDate(importCandidate.getIndexedDate())
                   .withPublishedDate(importCandidate.getPublishedDate())
                   .withStatus(importCandidate.getStatus())
                   .withPublishedDate(importCandidate.getPublishedDate())
                   .withAssociatedArtifactsList(importCandidate.getAssociatedArtifacts())
                   .withPublisher(importCandidate.getPublisher())
                   .withLink(importCandidate.getLink())
                   .withProjects(importCandidate.getProjects())
                   .withEntityDescription(importCandidate.getEntityDescription())
                   .withFilesEntries(getFileEntriesFromPublication(importCandidate))
                   .withDoi(importCandidate.getDoi())
                   .withHandle(importCandidate.getHandle())
                   .withAdditionalIdentifiers(importCandidate.getAdditionalIdentifiers())
                   .withSubjects(importCandidate.getSubjects())
                   .withFundings(importCandidate.getFundings())
                   .withRightsHolder(importCandidate.getRightsHolder())
                   .withImportStatus(importCandidate.getImportStatus())
                   .withPublicationNotes(importCandidate.getPublicationNotes())
                   .withDuplicateOf(importCandidate.getDuplicateOf())
                   .withCuratingInstitutions(importCandidate.getCuratingInstitutions())
                   .withImportDetails(importCandidate.getImportDetails())
                   .build();
    }

    public static ResourceBuilder builder() {
        return new ResourceBuilder();
    }

    public static Resource fromImportCandidate(ImportCandidate importCandidate) {
        return Optional.ofNullable(importCandidate).map(Resource::convertToResource).orElse(null);
    }

    public Publication persistNew(ResourceService resourceService, UserInstance userInstance)
        throws BadRequestException {
        return resourceService.createPublication(userInstance, this.toPublication());
    }

    public Resource importResource(ResourceService resourceService, ImportSource importSource) {
        var now = Instant.now();
        this.setCreatedDate(now);
        this.setModifiedDate(now);
        this.setPublishedDate(now);
        this.setIdentifier(SortableIdentifier.next());
        this.setStatus(PUBLISHED);
        var userInstance = UserInstance.fromPublication(this.toPublication());
        this.setResourceEvent(ImportedResourceEvent.fromImportSource(importSource, userInstance, now));
        return resourceService.importResource(this, importSource);
    }

    public void updateResourceFromImport(ResourceService resourceService, ImportSource importSource) {
        var userInstance = UserInstance.fromPublication(this.toPublication());
        this.setResourceEvent(ImportedResourceEvent.fromImportSource(importSource, userInstance, Instant.now()));
        resourceService.updateResource(this);
    }

    public List<LogEntry> fetchLogEntries(ResourceService resourceService) {
        return resourceService.getLogEntriesForResource(this);
    }

    public Optional<Resource> fetch(ResourceService resourceService) {
        return attempt(() -> resourceService.getResourceByIdentifier(this.getIdentifier())).toOptional();
    }

    public void publish(ResourceService resourceService, UserInstance userInstance) {
        fetch(resourceService)
            .filter(Resource::isNotPublished)
            .ifPresent(resource -> resource.publish(userInstance, resourceService));
    }

    private void publish(UserInstance userInstance, ResourceService resourceService) {
        publish(userInstance);
        resourceService.updateResource(this);
    }

    private void publish(UserInstance userInstance) {
        if (isNotPublishable()) {
            throw new IllegalStateException("Resource is not publishable!");
        } else if (this.isNotPublished()) {
            this.setStatus(PUBLISHED);
            var currentTime = Instant.now();
            this.setPublishedDate(currentTime);
            this.setResourceEvent(PublishedResourceEvent.create(userInstance, currentTime));
        }
    }

    private boolean isNotPublishable() {
        return !PUBLISHABLE_STATUSES.contains(this.getStatus())
               || Optional.ofNullable(this.getEntityDescription()).map(EntityDescription::getMainTitle).isEmpty();
    }

    private boolean isNotPublished() {
        return !isPublished();
    }

    private boolean isPublished() {
        return PUBLISHED.equals(this.getStatus());
    }

    public void republish(ResourceService resourceService, UserInstance userInstance) {
        fetch(resourceService)
            .filter(Resource::isNotPublished)
            .ifPresent(resource -> resource.republish(userInstance, resourceService));
    }

    private void republish(UserInstance userInstance, ResourceService resourceService) {
        republish(userInstance);
        resourceService.updateResource(this);
    }

    private void republish(UserInstance userInstance) {
        if (!UNPUBLISHED.equals(this.getStatus())) {
            throw new IllegalStateException("Only unpublished resource can be republished!");
        }
        this.setStatus(PUBLISHED);
        var timestamp = Instant.now();
        this.setPublishedDate(timestamp);
        this.setResourceEvent(RepublishedResourceEvent.create(userInstance, timestamp));
    }

    public URI getDuplicateOf() {
        return duplicateOf;
    }

    public void setDuplicateOf(URI duplicateOf) {
        this.duplicateOf = duplicateOf;
    }

    public Owner getResourceOwner() {
        return resourceOwner;
    }

    public void setResourceOwner(Owner resourceOwner) {
        this.resourceOwner = resourceOwner;
    }

    public Set<AdditionalIdentifierBase> getAdditionalIdentifiers() {
        return nonNull(additionalIdentifiers) ? additionalIdentifiers : Collections.emptySet();
    }

    public void setAdditionalIdentifiers(Set<AdditionalIdentifierBase> additionalIdentifiers) {
        this.additionalIdentifiers = additionalIdentifiers;
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    /**
     * This gets the import status for importCandidate and should be null in other context.
     *
     * @return importStatus if Resource is an ImportCandidate
     */
    public Optional<ImportStatus> getImportStatus() {
        return Optional.ofNullable(importStatus);
    }

    public void setImportStatus(ImportStatus importStatus) {
        this.importStatus = importStatus;
    }

    public List<PublicationNoteBase> getPublicationNotes() {
        return nonNull(publicationNotes) ? publicationNotes : List.of();
    }

    public void setPublicationNotes(List<PublicationNoteBase> publicationNotes) {
        this.publicationNotes = publicationNotes;
    }

    public Set<CuratingInstitution> getCuratingInstitutions() {
        return nonNull(this.curatingInstitutions) ? this.curatingInstitutions : Collections.emptySet();
    }

    public void setCuratingInstitutions(Set<CuratingInstitution> curatingInstitutions) {
        this.curatingInstitutions = curatingInstitutions;
    }

    @Override
    public Publication toPublication(ResourceService resourceService) {
        return toPublication();
    }

    public Publication toPublication() {
        return new Publication.Builder()
                   .withIdentifier(getIdentifier())
                   .withResourceOwner(extractResourceOwner())
                   .withStatus(getStatus())
                   .withCreatedDate(getCreatedDate())
                   .withModifiedDate(getModifiedDate())
                   .withIndexedDate(getIndexedDate())
                   .withPublisher(getPublisher())
                   .withPublishedDate(getPublishedDate())
                   .withLink(getLink())
                   .withProjects(getProjects())
                   .withEntityDescription(getEntityDescription())
                   .withDoi(getDoi())
                   .withHandle(getHandle())
                   .withAdditionalIdentifiers(getAdditionalIdentifiers())
                   .withAssociatedArtifacts(getAssociatedArtifacts())
                   .withSubjects(getSubjects())
                   .withFundings(getFundings())
                   .withRightsHolder(getRightsHolder())
                   .withPublicationNotes(getPublicationNotes())
                   .withDuplicateOf(getDuplicateOf())
                   .withCuratingInstitutions(getCuratingInstitutions())
                   .withImportDetails(getImportDetails())
                   .build();
    }

    public ImportCandidate toImportCandidate() {
        return new ImportCandidate.Builder()
                   .withIdentifier(getIdentifier())
                   .withResourceOwner(extractResourceOwner())
                   .withStatus(getStatus())
                   .withCreatedDate(getCreatedDate())
                   .withModifiedDate(getModifiedDate())
                   .withIndexedDate(getIndexedDate())
                   .withPublisher(getPublisher())
                   .withPublishedDate(getPublishedDate())
                   .withLink(getLink())
                   .withProjects(getProjects())
                   .withEntityDescription(getEntityDescription())
                   .withDoi(getDoi())
                   .withHandle(getHandle())
                   .withAdditionalIdentifiers(getAdditionalIdentifiers())
                   .withAssociatedArtifacts(getAssociatedArtifacts())
                   .withSubjects(getSubjects())
                   .withFundings(getFundings())
                   .withRightsHolder(getRightsHolder())
                   .withImportStatus(getImportStatus().orElse(null))
                   .withPublicationNotes(getPublicationNotes())
                   .withDuplicateOf(getDuplicateOf())
                   .withCuratingInstitutions(getCuratingInstitutions())
                   .withImportDetails(getImportDetails())
                   .build();
    }

    private ResourceOwner extractResourceOwner() {
        return Optional.ofNullable(getResourceOwner()).map(Owner::toResourceOwner).orElse(null);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }

    @Override
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @Override
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @JsonIgnore
    @Override
    public User getOwner() {
        return getResourceOwner().getUser();
    }

    @Override
    @JsonIgnore
    public URI getCustomerId() {
        return nonNull(this.getPublisher()) ? this.getPublisher().getId() : null;
    }

    @Override
    public Dao toDao() {
        return new ResourceDao(this);
    }

    @Override
    public String getStatusString() {
        return nonNull(getStatus()) ? getStatus().toString() : null;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public void setStatus(PublicationStatus status) {
        this.status = status;
    }

    public Organization getPublisher() {
        return publisher;
    }

    public void setPublisher(Organization publisher) {
        this.publisher = publisher;
    }

    public Instant getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(Instant publishedDate) {
        this.publishedDate = publishedDate;
    }

    public Instant getIndexedDate() {
        return indexedDate;
    }

    public void setIndexedDate(Instant indexedDate) {
        this.indexedDate = indexedDate;
    }

    public URI getLink() {
        return link;
    }

    public void setLink(URI link) {
        this.link = link;
    }

    public AssociatedArtifactList getAssociatedArtifacts() {
        return nonNull(associatedArtifacts)
                   ? associatedArtifacts
                   : new AssociatedArtifactList(new ArrayList<>());
    }

    public void setAssociatedArtifacts(AssociatedArtifactList associatedArtifacts) {
        this.associatedArtifacts = associatedArtifacts;
    }

    public List<ResearchProject> getProjects() {
        return nonNull(projects) ? projects : Collections.emptyList();
    }

    public void setProjects(List<ResearchProject> projects) {
        this.projects = projects;
    }

    public EntityDescription getEntityDescription() {
        return entityDescription;
    }

    public void setEntityDescription(EntityDescription entityDescription) {
        this.entityDescription = entityDescription;
    }

    public URI getDoi() {
        return doi;
    }

    public void setDoi(URI doi) {
        this.doi = doi;
    }

    public URI getHandle() {
        return handle;
    }

    public void setHandle(URI handle) {
        this.handle = handle;
    }

    public List<ImportDetail> getImportDetails() {
        return nonNull(importDetails) ? importDetails : Collections.emptyList();
    }

    public void setImportDetails(Collection<ImportDetail> importDetails) {
        this.importDetails = nonNull(importDetails) ? new ArrayList<>(importDetails) : new ArrayList<>();
    }

    public ResourceBuilder copy() {
        return Resource.builder()
                   .withIdentifier(getIdentifier())
                   .withStatus(getStatus())
                   .withResourceOwner(Owner.fromResourceOwner(extractResourceOwner()))
                   .withPublisher(getPublisher())
                   .withCreatedDate(getCreatedDate())
                   .withModifiedDate(getModifiedDate())
                   .withPublishedDate(getPublishedDate())
                   .withIndexedDate(getIndexedDate())
                   .withLink(getLink())
                   .withAssociatedArtifactsList(getAssociatedArtifacts())
                   .withFilesEntries(getFileEntries())
                   .withProjects(getProjects())
                   .withEntityDescription(getEntityDescription())
                   .withDoi(getDoi())
                   .withHandle(getHandle())
                   .withAdditionalIdentifiers(getAdditionalIdentifiers())
                   .withSubjects(getSubjects())
                   .withFundings(getFundings())
                   .withPublicationNotes(getPublicationNotes())
                   .withDuplicateOf(getDuplicateOf())
                   .withRightsHolder(getRightsHolder())
                   .withCuratingInstitutions(getCuratingInstitutions())
                   .withImportDetails(getImportDetails())
                   .withResourceEvent(getResourceEvent());
    }

    public List<URI> getSubjects() {
        return nonNull(subjects) ? subjects : Collections.emptyList();
    }

    public void setSubjects(List<URI> subjects) {
        this.subjects = subjects;
    }

    public List<Funding> getFundings() {
        return nonNull(fundings) ? fundings : Collections.emptyList();
    }

    public void setFundings(List<Funding> fundings) {
        this.fundings = new FundingList(fundings);
    }

    public String getRightsHolder() {
        return rightsHolder;
    }

    public void setRightsHolder(String rightsHolder) {
        this.rightsHolder = rightsHolder;
    }

    /**
     * Calculates hashcode without considering the row version.
     *
     * @return the hashcode.
     */
    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getStatus(), getResourceOwner(), getPublisher(), getCreatedDate(),
                            getModifiedDate(), getPublishedDate(), getIndexedDate(), getLink(),
                            getProjects(), getEntityDescription(), getDoi(), getHandle(), getAdditionalIdentifiers(),
                            getSubjects(), getFundings(), getAssociatedArtifacts(), getPublicationNotes(),
                            getDuplicateOf(), getCuratingInstitutions(), getImportDetails());
    }

    /**
     * It compares two Resources ignoring the row version.
     *
     * @param o the other Resource.
     * @return true if the two Resources are equivalent without considering the row version, false otherwise.
     */
    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Resource)) {
            return false;
        }
        Resource resource = (Resource) o;
        return Objects.equals(getIdentifier(), resource.getIdentifier())
               && getStatus() == resource.getStatus()
               && Objects.equals(getResourceOwner(), resource.getResourceOwner())
               && Objects.equals(getPublisher(), resource.getPublisher())
               && Objects.equals(getCreatedDate(), resource.getCreatedDate())
               && Objects.equals(getModifiedDate(), resource.getModifiedDate())
               && Objects.equals(getPublishedDate(), resource.getPublishedDate())
               && Objects.equals(getIndexedDate(), resource.getIndexedDate())
               && Objects.equals(getLink(), resource.getLink())
               && Objects.equals(getProjects(), resource.getProjects())
               && Objects.equals(getEntityDescription(), resource.getEntityDescription())
               && Objects.equals(getDoi(), resource.getDoi())
               && Objects.equals(getHandle(), resource.getHandle())
               && Objects.equals(getAdditionalIdentifiers(), resource.getAdditionalIdentifiers())
               && Objects.equals(getAssociatedArtifacts(), resource.getAssociatedArtifacts())
               && Objects.equals(getFundings(), resource.getFundings())
               && Objects.equals(getPublicationNotes(), resource.getPublicationNotes())
               && Objects.equals(getDuplicateOf(), resource.getDuplicateOf())
               && Objects.equals(getSubjects(), resource.getSubjects())
               && Objects.equals(getCuratingInstitutions(), resource.getCuratingInstitutions())
               && Objects.equals(getImportDetails(), resource.getImportDetails());
    }

    public Stream<TicketEntry> fetchAllTickets(ResourceService resourceService) {
        return resourceService.fetchAllTicketsForResource(this);
    }
}

