package no.unit.nva.model;

import static java.util.Objects.hash;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationStatus.DRAFT_FOR_DELETION;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.WithIdentifier;
import no.unit.nva.WithInternal;
import no.unit.nva.WithMetadata;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.config.ResourcesBuildConfig;
import no.unit.nva.model.exceptions.InvalidPublicationStatusTransitionException;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.funding.FundingList;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.TooManyFields", "PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class Publication
    implements WithIdentifier, WithInternal, WithMetadata, WithCopy<Publication.Builder> {

    public static final Map<PublicationStatus, List<PublicationStatus>> validStatusTransitionsMap = Map.of(
        PublicationStatus.NEW, List.of(PublicationStatus.DRAFT),
        PublicationStatus.DRAFT, List.of(PublicationStatus.PUBLISHED, DRAFT_FOR_DELETION)
    );

    private static final String MODEL_VERSION = ResourcesBuildConfig.RESOURCES_MODEL_VERSION;
    private static final String BASE_URI = "__BASE_URI__";
    private static final String PUBLICATION_CONTEXT = stringFromResources(Path.of("publicationContext.json"));
    private static final String ONTOLOGY = stringFromResources(Path.of("publication-ontology.ttl"));
    public static final String MUST_PRESERVE_EXISTING_IMPORT_DETAILS = "Must preserve existing importDetails";

    private SortableIdentifier identifier;
    private PublicationStatus status;
    private ResourceOwner resourceOwner;
    private Organization publisher;
    private Instant createdDate;
    private Instant modifiedDate;
    private Instant publishedDate;
    private Instant indexedDate;
    private URI handle;
    private URI doi;
    private URI link;
    private EntityDescription entityDescription;
    private List<ResearchProject> projects;
    private FundingList fundings;
    private Set<AdditionalIdentifierBase> additionalIdentifiers;
    private List<URI> subjects;
    private AssociatedArtifactList associatedArtifacts;
    private String rightsHolder;
    private URI duplicateOf;

    private List<PublicationNoteBase> publicationNotes;
    private Set<CuratingInstitution> curatingInstitutions;
    private List<ImportDetail> importDetails;

    public Publication() {
        // Default constructor, use setters.
    }

    public URI getDuplicateOf() {
        return duplicateOf;
    }

    public void setDuplicateOf(URI duplicateOf) {
        this.duplicateOf = duplicateOf;
    }

    public Set<AdditionalIdentifierBase> getAdditionalIdentifiers() {
        return nonNull(additionalIdentifiers) ? additionalIdentifiers : Collections.emptySet();
    }

    public void setAdditionalIdentifiers(Set<AdditionalIdentifierBase> additionalIdentifiers) {
        this.additionalIdentifiers = additionalIdentifiers;
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
    public PublicationStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(PublicationStatus status) {
        this.status = status;
    }

    @Override
    public URI getHandle() {
        return handle;
    }

    @Override
    public void setHandle(URI handle) {
        this.handle = handle;
    }

    @Override
    public Instant getPublishedDate() {
        return publishedDate;
    }

    @Override
    public void setPublishedDate(Instant publishedDate) {
        this.publishedDate = publishedDate;
    }

    @Override
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @Override
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @Override
    public ResourceOwner getResourceOwner() {
        return resourceOwner;
    }

    @Override
    public void setResourceOwner(ResourceOwner resourceOwner) {
        this.resourceOwner = resourceOwner;
    }

    @Override
    public Instant getIndexedDate() {
        return indexedDate;
    }

    @Override
    public void setIndexedDate(Instant indexedDate) {
        this.indexedDate = indexedDate;
    }

    @Override
    public URI getLink() {
        return link;
    }

    @Override
    public void setLink(URI link) {
        this.link = link;
    }

    @Override
    public Organization getPublisher() {
        return publisher;
    }

    @Override
    public void setPublisher(Organization publisher) {
        this.publisher = publisher;
    }

    @Override
    public URI getDoi() {
        return doi;
    }

    @Override
    public void setDoi(URI doi) {
        this.doi = doi;
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public EntityDescription getEntityDescription() {
        return entityDescription;
    }

    @Override
    public void setEntityDescription(EntityDescription entityDescription) {
        this.entityDescription = entityDescription;
    }

    @Override
    public List<ResearchProject> getProjects() {
        return nonNull(projects) ? projects : Collections.emptyList();
    }

    @Override
    public void setProjects(List<ResearchProject> projects) {
        this.projects = projects;
    }

    @Override
    public List<URI> getSubjects() {
        return nonNull(subjects) ? subjects : Collections.emptyList();
    }

    @Override
    public void setSubjects(List<URI> subjects) {
        this.subjects = subjects;
    }

    @Override
    public List<Funding> getFundings() {
        return nonNull(fundings) ? fundings : Collections.emptyList();
    }

    @Override
    public void setFundings(List<Funding> fundings) {
        this.fundings = new FundingList(fundings);
    }

    @Override
    public String getRightsHolder() {
        return rightsHolder;
    }

    @Override
    public void setRightsHolder(String rightsHolder) {
        this.rightsHolder = rightsHolder;
    }

    @JsonProperty("modelVersion")
    public String getModelVersion() {
        return MODEL_VERSION;
    }

    @JsonProperty("modelVersion")
    public void setModelVersion() {
        // NO-OP
    }

    @JsonGetter
    public List<PublicationNoteBase> getPublicationNotes() {
        return nonNull(publicationNotes) ? publicationNotes : Collections.emptyList();
    }

    public void setPublicationNotes(List<PublicationNoteBase> publicationNotes) {
        this.publicationNotes = publicationNotes;
    }

    public AssociatedArtifactList getAssociatedArtifacts() {
        return nonNull(associatedArtifacts)
                   ? associatedArtifacts
                   : AssociatedArtifactList.empty();
    }

    public void setAssociatedArtifacts(AssociatedArtifactList associatedArtifacts) {
        this.associatedArtifacts = associatedArtifacts;
    }

    @Override
    public Builder copy() {
        return new Builder()
                   .withIdentifier(getIdentifier())
                   .withStatus(getStatus())
                   .withResourceOwner(getResourceOwner())
                   .withPublisher(getPublisher())
                   .withCreatedDate(getCreatedDate())
                   .withModifiedDate(getModifiedDate())
                   .withPublishedDate(getPublishedDate())
                   .withIndexedDate(getIndexedDate())
                   .withHandle(getHandle())
                   .withDoi(getDoi())
                   .withLink(getLink())
                   .withEntityDescription(getEntityDescription())
                   .withProjects(getProjects())
                   .withFundings(getFundings())
                   .withAdditionalIdentifiers(getAdditionalIdentifiers())
                   .withAssociatedArtifacts(getAssociatedArtifacts())
                   .withSubjects(getSubjects())
                   .withFundings(getFundings())
                   .withRightsHolder(getRightsHolder())
                   .withPublicationNotes(getPublicationNotes())
                   .withDuplicateOf(getDuplicateOf())
                   .withCuratingInstitutions(getCuratingInstitutions())
                   .withImportDetails(getImportDetails());
    }

    /**
     * Updates the status of the publication using rules for valid status transitions.
     *
     * @param nextStatus the status to update to
     * @throws InvalidPublicationStatusTransitionException if the status transition is not allowed
     */
    public void updateStatus(PublicationStatus nextStatus) throws InvalidPublicationStatusTransitionException {
        verifyStatusTransition(nextStatus);
        setStatus(nextStatus);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return hash(getIdentifier(), getStatus(), getPublisher(), getCreatedDate(), getModifiedDate(),
                    getPublishedDate(), getIndexedDate(), getHandle(), getDoi(), getLink(),
                    getEntityDescription(), getProjects(), getFundings(), getAdditionalIdentifiers(), getSubjects(),
                    getAssociatedArtifacts(), getRightsHolder(), getPublicationNotes(), getDuplicateOf(),
                    getCuratingInstitutions(), getImportDetails());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Publication that)) {
            return false;
        }
        boolean firstHalf = Objects.equals(getIdentifier(), that.getIdentifier())
                            && getStatus() == that.getStatus()
                            && Objects.equals(getResourceOwner(), that.getResourceOwner())
                            && Objects.equals(getPublisher(), that.getPublisher())
                            && Objects.equals(getCreatedDate(), that.getCreatedDate())
                            && Objects.equals(getModifiedDate(), that.getModifiedDate())
                            && Objects.equals(getPublishedDate(), that.getPublishedDate());
        boolean secondHalf = Objects.equals(getIndexedDate(), that.getIndexedDate())
                             && Objects.equals(getHandle(), that.getHandle())
                             && Objects.equals(getDoi(), that.getDoi())
                             && Objects.equals(getLink(), that.getLink())
                             && Objects.equals(getEntityDescription(), that.getEntityDescription())
                             && new HashSet<>(getAssociatedArtifacts()).containsAll(that.getAssociatedArtifacts())
                             && Objects.equals(getProjects(), that.getProjects())
                             && Objects.equals(getFundings(), that.getFundings())
                             && new HashSet<>(getAdditionalIdentifiers()).containsAll(that.getAdditionalIdentifiers())
                             && Objects.equals(getSubjects(), that.getSubjects())
                             && Objects.equals(getRightsHolder(), that.getRightsHolder())
                             && Objects.equals(getPublicationNotes(), that.getPublicationNotes())
                             && Objects.equals(getDuplicateOf(), that.getDuplicateOf())
                             && Objects.equals(getCuratingInstitutions(), that.getCuratingInstitutions())
                             && Objects.equals(getImportDetails(), that.getImportDetails());
        return firstHalf && secondHalf;
    }

    @Override
    public String toString() {
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(this)).orElseThrow();
    }

    @JsonIgnore
    public static String getJsonLdContext(URI baseUri) {
        return PUBLICATION_CONTEXT.replace(BASE_URI, baseUri.toString());
    }

    @JsonIgnore
    public static String getOntology(URI baseUri) {
        return ONTOLOGY.replace(BASE_URI, baseUri.toString());
    }

    @JsonIgnore
    public boolean isPublishable() {
        return !DRAFT_FOR_DELETION.equals(getStatus()) && hasMainTitle();
    }

    public boolean satisfiesFindableDoiRequirements() {
        return FindableDoiRequirementsValidator.meetsFindableDoiRequirements(this);
    }

    public Set<CuratingInstitution> getCuratingInstitutions() {
        return nonNull(curatingInstitutions) ? curatingInstitutions : Collections.emptySet();
    }

    public void setCuratingInstitutions(Set<CuratingInstitution> curatingInstitutions) {
        this.curatingInstitutions = curatingInstitutions;
    }


    @Override
    public List<ImportDetail> getImportDetails() {
        return nonNull(importDetails) ? importDetails : Collections.emptyList();
    }

    @Override
    public void setImportDetails(Collection<ImportDetail> importDetails) {
        if (importDetails == null || !new HashSet<>(importDetails).containsAll(getImportDetails())) {
            throw new IllegalArgumentException(MUST_PRESERVE_EXISTING_IMPORT_DETAILS);
        }

        this.importDetails = new ArrayList<>(importDetails);
    }

    public void addImportDetail(ImportDetail importDetail) {
        if (isNull(importDetail)) {
            return;
        }

        if (isNull(importDetails)) {
            importDetails = new ArrayList<>();
        }

        importDetails.add(importDetail);
    }

    public Optional<File> getFile(UUID fileIdentifier) {
        return getAssociatedArtifacts().stream()
            .filter(File.class::isInstance)
            .map(File.class::cast)
            .filter(element -> fileIdentifier.equals(element.getIdentifier()))
            .findFirst();
    }

    public long getPendingOpenFileCount() {
        return getAssociatedArtifacts().stream()
                   .filter(PendingOpenFile.class::isInstance)
                   .count();
    }

    private void verifyStatusTransition(PublicationStatus nextStatus)
        throws InvalidPublicationStatusTransitionException {
        final PublicationStatus currentStatus = getStatus();
        if (!validStatusTransitionsMap.get(currentStatus).contains(nextStatus)) {
            throw new InvalidPublicationStatusTransitionException(currentStatus, nextStatus);
        }
    }

    private boolean hasMainTitle() {
        return Optional.ofNullable(getEntityDescription())
                   .map(EntityDescription::getMainTitle)
                   .filter(string -> !StringUtils.isEmpty(string))
                   .isPresent();
    }

    public static final class Builder {

        private final Publication publication;

        public Builder() {
            publication = new Publication();
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            publication.setIdentifier(identifier);
            return this;
        }

        public Builder withStatus(PublicationStatus status) {
            publication.setStatus(status);
            return this;
        }

        public Builder withPublisher(Organization publisher) {
            publication.setPublisher(publisher);
            return this;
        }

        public Builder withCreatedDate(Instant createdDate) {
            publication.setCreatedDate(createdDate);
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            publication.setModifiedDate(modifiedDate);
            return this;
        }

        public Builder withPublishedDate(Instant publishedDate) {
            publication.setPublishedDate(publishedDate);
            return this;
        }

        public Builder withIndexedDate(Instant indexedDate) {
            publication.setIndexedDate(indexedDate);
            return this;
        }

        public Builder withHandle(URI handle) {
            publication.setHandle(handle);
            return this;
        }

        public Builder withDoi(URI doi) {
            publication.setDoi(doi);
            return this;
        }

        public Builder withLink(URI link) {
            publication.setLink(link);
            return this;
        }

        public Builder withEntityDescription(EntityDescription entityDescription) {
            publication.setEntityDescription(entityDescription);
            return this;
        }

        public Builder withAssociatedArtifacts(List<AssociatedArtifact> associatedArtifacts) {
            publication.setAssociatedArtifacts(new AssociatedArtifactList(associatedArtifacts));
            return this;
        }

        public Builder withProjects(List<ResearchProject> projects) {
            publication.setProjects(projects);
            return this;
        }

        public Builder withFundings(List<Funding> fundings) {
            publication.setFundings(fundings);
            return this;
        }

        public Builder withAdditionalIdentifiers(Set<AdditionalIdentifierBase> additionalIdentifiers) {
            publication.setAdditionalIdentifiers(additionalIdentifiers);
            return this;
        }

        public Builder withSubjects(List<URI> subjects) {
            publication.setSubjects(subjects);
            return this;
        }

        public Builder withResourceOwner(ResourceOwner randomResourceOwner) {
            publication.setResourceOwner(randomResourceOwner);
            return this;
        }


        public Builder withRightsHolder(String rightsHolder) {
            this.publication.setRightsHolder(rightsHolder);
            return this;
        }

        public Builder withPublicationNotes(List<PublicationNoteBase> publicationNotes) {
            publication.setPublicationNotes(publicationNotes);
            return this;
        }

        public Builder withDuplicateOf(URI duplicateOf) {
            publication.setDuplicateOf(duplicateOf);
            return this;
        }

        public Builder withCuratingInstitutions(Set<CuratingInstitution> curatingInstitutions) {
            publication.setCuratingInstitutions(curatingInstitutions);
            return this;
        }

        public Builder withImportDetails(List<ImportDetail> importDetails) {
            publication.setImportDetails(importDetails);
            return this;
        }

        public Publication build() {
            return publication;
        }
    }
}
