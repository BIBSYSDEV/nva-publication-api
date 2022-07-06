package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.storage.model.daos.Dao;
import no.unit.nva.publication.storage.model.daos.PublishingRequestDao;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class PublishingRequest
    implements WithIdentifier,
               RowLevelSecurity,
               WithStatus,
               DataEntry,
               ConnectedToResource {

    public static final String TYPE = "PublishingRequest";
    public static final String STATUS_FIELD = "status";
    public static final String MODIFIED_DATE_FIELD = "modifiedDate";
    public static final String RESOURCE_STATUS_FIELD = "resourceStatus";
    public static final SortableIdentifier UNDEFINED_IDENTIFIER = null;

    @JsonProperty
    private SortableIdentifier identifier;
    @JsonProperty
    private SortableIdentifier resourceIdentifier;
    @JsonProperty(STATUS_FIELD)
    private PublishingRequestStatus status;
    @JsonProperty("customerId")
    private URI customerId;
    @JsonProperty("owner")
    private String owner;
    @JsonProperty
    private Instant modifiedDate;
    @JsonProperty
    private Instant createdDate;
    private String rowVersion;

    public PublishingRequest() {
    }

    public static PublishingRequest newPublishingRequestResource(Resource resource) {
        return newPublishingRequestResource(SortableIdentifier.next(), resource, Clock.systemDefaultZone().instant());
    }

    public static PublishingRequest create(UserInstance userInstance,
                                           SortableIdentifier publicationIdentifier,
                                           SortableIdentifier publishingRequestIdentifier,
                                           PublishingRequestStatus publishingRequestStatus) {
        PublishingRequest newPublishingRequest =
            createPublishingRequestIdentifyingObject(userInstance, publicationIdentifier, publishingRequestIdentifier);
        newPublishingRequest.setStatus(publishingRequestStatus);
        return newPublishingRequest;
    }

    public static PublishingRequest newPublishingRequestResource(SortableIdentifier requestIdentifier,
                                                                 Resource resource,
                                                                 Instant now) {
        var newPublishingRequest = extractDataFromResource(resource);
        newPublishingRequest.setIdentifier(requestIdentifier);
        newPublishingRequest.setModifiedDate(now);
        newPublishingRequest.setCreatedDate(now);
        newPublishingRequest.setRowVersion(DataEntry.nextRowVersion());
        return newPublishingRequest;
    }

    public static PublishingRequest fromPublication(Publication publication, SortableIdentifier requestIdentifier) {
        var newPublishingRequest = extractDataFromResource(Resource.fromPublication(publication));
        newPublishingRequest.setIdentifier(requestIdentifier);
        return newPublishingRequest;
    }

    public static PublishingRequest createQuery(UserInstance userInstance,
                                                SortableIdentifier publicationIdentifier,
                                                SortableIdentifier publishingRequestIdentifier) {
        return createPublishingRequestIdentifyingObject(userInstance,
                                                        publicationIdentifier,
                                                        publishingRequestIdentifier);
    }

    @Override
    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    public void setResourceIdentifier(SortableIdentifier resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    @Override
    public Publication toPublication() {

        return new Publication.Builder()
            .withIdentifier(getResourceIdentifier())
            .withResourceOwner(new ResourceOwner(getOwner(), null))
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
        return new PublishingRequestDao(this);
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

    public PublishingRequestStatus getStatus() {
        return status;
    }

    public void setStatus(PublishingRequestStatus status) {
        this.status = status;
    }

    @Override
    public String getStatusString() {
        return status.name();
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
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getResourceIdentifier(), getStatus(), getCustomerId(), getOwner(),
                            getModifiedDate(), getCreatedDate(), getRowVersion());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublishingRequest)) {
            return false;
        }
        PublishingRequest that = (PublishingRequest) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getResourceIdentifier(), that.getResourceIdentifier())
               && getStatus() == that.getStatus()
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getRowVersion(), that.getRowVersion());
    }

    private static PublishingRequest extractDataFromResource(Resource resource) {
        var userInstance = UserInstance.create(resource.getResourceOwner().getOwner(), resource.getPublisher().getId());
        return PublishingRequest.create(userInstance,
                                        resource.getIdentifier(),
                                        UNDEFINED_IDENTIFIER,
                                        PublishingRequestStatus.PENDING);
    }

    private static PublishingRequest createPublishingRequestIdentifyingObject(UserInstance userInstance,
                                                                              SortableIdentifier publicationIdentifier,
                                                                              SortableIdentifier publishingRequestIdentifier) {
        var newPublishingRequest = new PublishingRequest();
        newPublishingRequest.setOwner(userInstance.getUserIdentifier());
        newPublishingRequest.setCustomerId(userInstance.getOrganizationUri());
        newPublishingRequest.setResourceIdentifier(publicationIdentifier);
        newPublishingRequest.setIdentifier(publishingRequestIdentifier);
        return newPublishingRequest;
    }
}
