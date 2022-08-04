package no.unit.nva.publication.model.business;

import static java.util.Objects.isNull;
import static no.unit.nva.publication.model.business.DoiRequestUtils.extractDataFromResource;
import static no.unit.nva.publication.model.business.Entity.nextVersion;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.model.storage.DoiRequestDao;
import no.unit.nva.publication.storage.model.exceptions.IllegalDoiRequestUpdate;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount", "PMD.TooManyFields"})
public class DoiRequest implements WithStatus, TicketEntry {
    
    public static final String RESOURCE_STATUS_FIELD = "resourceStatus";
    public static final String STATUS_FIELD = "status";
    public static final String MODIFIED_DATE_FIELD = "modifiedDate";
    public static final String TYPE = "DoiRequest";
    
    public static final String MISSING_RESOURCE_REFERENCE_ERROR = "Resource identifier cannot be null or empty";
    
    public static final String RESOURCE_IDENTIFIER_MISMATCH_ERROR = "Resource identifier mismatch";
    private static final URI UNKNOWN_USER_AFFILIATION = null;
    public static final String WRONG_PUBLICATION_STATUS_ERROR =
        "DoiRequests may only be created for publications with statuses %s";
    public static final Set<PublicationStatus> ACCEPTABLE_PUBLICATION_STATUSES = Set.of(PublicationStatus.PUBLISHED,
        PublicationStatus.DRAFT);
    
    private SortableIdentifier identifier;
    @JsonProperty
    private SortableIdentifier resourceIdentifier;
    @JsonProperty(STATUS_FIELD)
    private DoiRequestStatus status;
    @JsonProperty(RESOURCE_STATUS_FIELD)
    private PublicationStatus resourceStatus;
    @JsonProperty
    private Instant modifiedDate;
    @JsonProperty
    @JsonAlias("date")
    private Instant createdDate;
    @JsonProperty("customerId")
    private URI customerId;
    @JsonProperty("owner")
    private String owner;
    @JsonProperty("resourceTitle")
    private String resourceTitle;
    @JsonProperty
    private Instant resourceModifiedDate;
    @JsonProperty
    private PublicationInstance<? extends Pages> resourcePublicationInstance;
    @JsonProperty
    private PublicationDate resourcePublicationDate;
    @JsonProperty
    private String resourcePublicationYear;
    @JsonProperty
    private URI doi;
    @JsonProperty
    private List<Contributor> contributors;
    private UUID version;
    
    public DoiRequest() {
    
    }
    
    public static DoiRequest fromPublication(Publication publication) {
        return newDoiRequestForResource(Resource.fromPublication(publication));
    }
    
    public static DoiRequest newDoiRequestForResource(Resource resource) {
        return newDoiRequestForResource(SortableIdentifier.next(), resource, Clock.systemDefaultZone().instant());
    }
    
    public static DoiRequest newDoiRequestForResource(Resource resource, Instant now) {
        return newDoiRequestForResource(SortableIdentifier.next(), resource, now);
    }
    
    public static DoiRequest newDoiRequestForResource(SortableIdentifier doiRequestIdentifier,
                                                      Resource resource,
                                                      Instant now) {
        
        DoiRequest doiRequest =
            extractDataFromResource(builder(), resource)
                .withIdentifier(doiRequestIdentifier)
                .withStatus(DoiRequestStatus.PENDING)
                .withModifiedDate(now)
                .withCreatedDate(now)
                .withDoi(resource.getDoi())
                .withRowVersion(nextVersion())
                .build();
        
        doiRequest.validate();
        return doiRequest;
    }
    
    @Override
    public String getType() {
        return DoiRequest.TYPE;
    }
    
    public static DoiRequestBuilder builder() {
        return new DoiRequestBuilder();
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
    public Publication toPublication() {
        
        Reference reference = new Reference.Builder()
            .withPublicationInstance(getResourcePublicationInstance())
            .build();
        
        EntityDescription entityDescription = new EntityDescription.Builder()
            .withMainTitle(getResourceTitle())
            .withDate(getResourcePublicationDate())
            .withReference(reference)
            .withContributors(getContributors())
            .build();
        
        Organization customer = new Organization.Builder()
            .withId(getCustomerId())
            .build();
        
        return new Publication.Builder()
            .withIdentifier(getResourceIdentifier())
            .withModifiedDate(getResourceModifiedDate())
            .withDoi(getDoi())
            .withStatus(getResourceStatus())
            .withEntityDescription(entityDescription)
            .withPublisher(customer)
            .withResourceOwner(new ResourceOwner(getOwner(), UNKNOWN_USER_AFFILIATION))
            
            .build();
    }
    
    @Override
    public UUID getVersion() {
        return version;
    }
    
    @Override
    public void setVersion(UUID rowVersion) {
        this.version = rowVersion;
    }
    
    @Override
    public DoiRequestDao toDao() {
        return new DoiRequestDao(this);
    }
    
    @Override
    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }
    
    @Override
    public void validateRequirements(Publication publication) throws ConflictException {
        if (publicationDoesNotHaveAnExpectedStatus(publication)) {
            throw new ConflictException(String.format(WRONG_PUBLICATION_STATUS_ERROR, ACCEPTABLE_PUBLICATION_STATUSES));
        }
    }
    
    private boolean publicationDoesNotHaveAnExpectedStatus(Publication publication) {
        return !ACCEPTABLE_PUBLICATION_STATUSES.contains(publication.getStatus());
    }
    
    public void setResourceIdentifier(SortableIdentifier resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }
    
    public DoiRequestStatus getStatus() {
        return status;
    }
    
    public void setStatus(DoiRequestStatus status) {
        this.status = status;
    }
    
    public PublicationStatus getResourceStatus() {
        return resourceStatus;
    }
    
    public void setResourceStatus(PublicationStatus resourceStatus) {
        this.resourceStatus = resourceStatus;
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
    public Instant getCreatedDate() {
        return createdDate;
    }
    
    @Override
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }
    
    @Override
    public URI getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }
    
    @Override
    public String getOwner() {
        return owner;
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    public String getResourceTitle() {
        return resourceTitle;
    }
    
    public void setResourceTitle(String resourceTitle) {
        this.resourceTitle = resourceTitle;
    }
    
    public Instant getResourceModifiedDate() {
        return resourceModifiedDate;
    }
    
    public void setResourceModifiedDate(Instant resourceModifiedDate) {
        this.resourceModifiedDate = resourceModifiedDate;
    }
    
    public PublicationInstance<? extends Pages> getResourcePublicationInstance() {
        return resourcePublicationInstance;
    }
    
    public void setResourcePublicationInstance(
        PublicationInstance<? extends Pages> resourcePublicationInstance) {
        this.resourcePublicationInstance = resourcePublicationInstance;
    }
    
    public PublicationDate getResourcePublicationDate() {
        return resourcePublicationDate;
    }
    
    public void setResourcePublicationDate(PublicationDate resourcePublicationDate) {
        this.resourcePublicationDate = resourcePublicationDate;
    }
    
    public String getResourcePublicationYear() {
        return resourcePublicationYear;
    }
    
    public void setResourcePublicationYear(String resourcePublicationYear) {
        this.resourcePublicationYear = resourcePublicationYear;
    }
    
    public URI getDoi() {
        return doi;
    }
    
    public void setDoi(URI doi) {
        this.doi = doi;
    }
    
    public List<Contributor> getContributors() {
        return contributors;
    }
    
    public void setContributors(List<Contributor> contributors) {
        this.contributors = contributors;
    }
    
    public DoiRequest update(Resource resource) {
        if (updateIsAboutTheSameResource(resource)) {
            return extractDataFromResource(this.copy(), resource).build();
        }
        throw new IllegalDoiRequestUpdate(RESOURCE_IDENTIFIER_MISMATCH_ERROR);
    }
    
    @Override
    public String getStatusString() {
        return Objects.nonNull(getStatus()) ? getStatus().toString() : null;
    }
    
    public DoiRequestBuilder copy() {
        return DoiRequest.builder()
            .withIdentifier(getIdentifier())
            .withResourceIdentifier(getResourceIdentifier())
            .withStatus(getStatus())
            .withResourceStatus(getResourceStatus())
            .withModifiedDate(getModifiedDate())
            .withCreatedDate(getCreatedDate())
            .withCustomerId(getCustomerId())
            .withOwner(getOwner())
            .withResourceTitle(getResourceTitle())
            .withResourceModifiedDate(getResourceModifiedDate())
            .withResourcePublicationInstance(getResourcePublicationInstance())
            .withResourcePublicationDate(getResourcePublicationDate())
            .withResourcePublicationYear(getResourcePublicationYear())
            .withDoi(getDoi())
            .withContributors(getContributors())
            .withRowVersion(getVersion());
    }
    
    public void validate() {
        if (isNull(resourceIdentifier)) {
            throw new IllegalArgumentException(MISSING_RESOURCE_REFERENCE_ERROR);
        }
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getResourceIdentifier(), getStatus(), getResourceStatus(),
            getModifiedDate(),
            getCreatedDate(), getCustomerId(), getOwner(), getResourceTitle(),
            getResourceModifiedDate(),
            getResourcePublicationInstance(), getResourcePublicationDate(),
            getResourcePublicationYear(),
            getDoi(), getContributors());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoiRequest)) {
            return false;
        }
        DoiRequest that = (DoiRequest) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getResourceIdentifier(), that.getResourceIdentifier())
               && getStatus() == that.getStatus()
               && getResourceStatus() == that.getResourceStatus()
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getResourceTitle(), that.getResourceTitle())
               && Objects.equals(getResourceModifiedDate(), that.getResourceModifiedDate())
               && Objects.equals(getResourcePublicationInstance(), that.getResourcePublicationInstance())
               && Objects.equals(getResourcePublicationDate(), that.getResourcePublicationDate())
               && Objects.equals(getResourcePublicationYear(), that.getResourcePublicationYear())
               && Objects.equals(getDoi(), that.getDoi())
               && Objects.equals(getContributors(), that.getContributors());
    }
    
    private boolean updateIsAboutTheSameResource(Resource resource) {
        return resource.getIdentifier().equals(this.getResourceIdentifier());
    }
}
