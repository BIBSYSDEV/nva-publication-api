package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.storage.model.daos.ApprovePublicationRequestDao;
import no.unit.nva.publication.storage.model.daos.Dao;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static no.unit.nva.publication.storage.model.DoiRequest.RESOURCE_STATUS_FIELD;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ApprovePublicationRequest
        implements WithIdentifier,
        RowLevelSecurity,
        WithStatus,
        DataEntry,
        ConnectedToResource {

    public static final String TYPE = "ApprovePublicationRequest";
    public static final String STATUS_FIELD = "status";


    @JsonProperty
    private SortableIdentifier identifier;
    @JsonProperty
    private SortableIdentifier resourceIdentifier;
    @JsonProperty(STATUS_FIELD)
    private ApprovePublicationRequestStatus status;
    @JsonProperty("customerId")
    private URI customerId;
    @JsonProperty("owner")
    private String owner;
    @JsonProperty("resourceTitle")
    private String resourceTitle;
    @JsonProperty(RESOURCE_STATUS_FIELD)
    private PublicationStatus resourceStatus;
    @JsonProperty
    private List<Contributor> contributors;
    @JsonProperty
    private Instant modifiedDate;
    @JsonProperty
    private Instant createdDate;
    private String rowVersion;

    public ApprovePublicationRequest() {
    }

    public static ApprovePublicationRequest newApprovePublicationRequestResource(Resource resource) {
        return newApprovePublicationRequestResource(SortableIdentifier.next(), resource, Clock.systemDefaultZone().instant());
    }

    public static ApprovePublicationRequest newApprovePublicationRequestResource(Resource resource, Instant now) {
        return newApprovePublicationRequestResource(SortableIdentifier.next(), resource, now);
    }


    public static ApprovePublicationRequest newApprovePublicationRequestResource(SortableIdentifier requestIdentifier,
                                                                  Resource resource,
                                                                  Instant now) {
        return extractDataFromResource(builder(), resource)
                .withIdentifier(requestIdentifier)
                .withResourceIdentifier(resource.getIdentifier())
                .withModifiedDate(now)
                .withCreatedDate(now)
                .withRowVersion(DataEntry.nextRowVersion())
                .build();

    }

    public static ApprovePublicationRequest fromPublication(Publication publication, SortableIdentifier requestIdentifier) {
        return  extractDataFromResource(builder(), Resource.fromPublication(publication))
                .withIdentifier(requestIdentifier)
                .build();
    }

    public void setResourceIdentifier(SortableIdentifier resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    @Override
    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    @Override
    public Publication toPublication() {

        return new Publication.Builder()
                .withIdentifier(getResourceIdentifier())
//                .withModifiedDate(getResourceModifiedDate())
                .withStatus(getResourceStatus())
//                .withEntityDescription(entityDescription)
//                .withPublisher(getCustomerId())
                .withResourceOwner(new ResourceOwner(getOwner(),null))
                .build();
    }

    @Override
    public String getRowVersion() {
        return rowVersion;
    }

    @Override
    public void setRowVersion(String rowVersion) {
        this.rowVersion = rowVersion;
    }


    @Override
    public Dao<?> toDao() {
        return new ApprovePublicationRequestDao(this);
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

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    public ApprovePublicationRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovePublicationRequestStatus status) {
        this.status = status;
    }

    @Override
    public String getStatusString() {
        return status.name();
    }

    public PublicationStatus getResourceStatus() {
        return resourceStatus;
    }

    public void setResourceStatus(PublicationStatus resourceStatus) {
        this.resourceStatus = resourceStatus;
    }

    public List<Contributor> getContributors() {
        return contributors;
    }

    public void setContributors(List<Contributor> contributors) {
        this.contributors = contributors;
    }

    public String getResourceTitle() {
        return resourceTitle;
    }

    public void setResourceTitle(String resourceTitle) {
        this.resourceTitle = resourceTitle;
    }



    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApprovePublicationRequest)) return false;
        ApprovePublicationRequest that = (ApprovePublicationRequest) o;
        return getIdentifier().equals(that.getIdentifier())
                && getResourceIdentifier().equals(that.getResourceIdentifier())
                && status == that.status && getCustomerId().equals(that.getCustomerId())
                && getOwner().equals(that.getOwner())
                && Objects.equals(getModifiedDate(), that.getModifiedDate())
                && getCreatedDate().equals(that.getCreatedDate())
                && Objects.equals(getRowVersion(), that.getRowVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(),
                getResourceIdentifier(),
                status,
                getCustomerId(),
                getOwner(),
                getModifiedDate(),
                getCreatedDate(),
                getRowVersion());
    }

    public static ApprovePublicationRequestBuilder builder() {
        return new ApprovePublicationRequestBuilder();
    }

    static ApprovePublicationRequestBuilder extractDataFromResource(ApprovePublicationRequestBuilder builder, Resource resource) {
        return builder
                .withResourceIdentifier(resource.getIdentifier())
                .withOwner(resource.getResourceOwner().getOwner())
                .withStatus(ApprovePublicationRequestStatus.PENDING)
                .withCustomerId(resource.getCustomerId())
//                .withResourcePublicationInstance(extractPublicationInstance(resource))
//                .withResourcePublicationYear(extractPublicationYear(resource))
                .withResourceStatus(resource.getStatus())
                .withResourceTitle(DoiRequestUtils.extractMainTitle(resource))
                .withContributors(DoiRequestUtils.extractContributors(resource))
                .withRowVersion(DataEntry.nextRowVersion());
    }

}
