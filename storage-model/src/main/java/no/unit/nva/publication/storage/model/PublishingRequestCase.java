package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.storage.model.daos.Dao;
import no.unit.nva.publication.storage.model.daos.PublishingRequestDao;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class PublishingRequestCase
    implements WithIdentifier,
               RowLevelSecurity,
               WithStatus,
               DataEntry,
               ConnectedToResource {

    public static final String TYPE = "PublishingRequestCase";
    public static final String STATUS_FIELD = "status";
    public static final String MODIFIED_DATE_FIELD = "modifiedDate";
    public static final String CREATED_DATE_FIELD = "createdDate";
    public static final String ROW_VERSION_FIELD = "rowVersion";
    public static final String OWNER_FIELD = "owner";
    public static final String CUSTOMER_ID_FIELD = "customerId";
    public static final String RESOURCE_IDENTIFIER_FIELD = "resourceIdentifier";
    public static final String IDENTIFIER_FIELD = "identifier";

    @JsonProperty(IDENTIFIER_FIELD)
    private SortableIdentifier identifier;
    @JsonProperty(RESOURCE_IDENTIFIER_FIELD)
    private SortableIdentifier resourceIdentifier;
    @JsonProperty(STATUS_FIELD)
    private PublishingRequestStatus status;
    @JsonProperty(CUSTOMER_ID_FIELD)
    private URI customerId;
    @JsonProperty(OWNER_FIELD)
    private String owner;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private Instant modifiedDate;
    @JsonProperty(CREATED_DATE_FIELD)
    private Instant createdDate;
    @JsonProperty(ROW_VERSION_FIELD)
    private String rowVersion;

    public PublishingRequestCase() {
    }

    public static PublishingRequestCase createOpeningCaseObject(UserInstance userInstance,
                                                                SortableIdentifier publicationIdentifier) {

        var openingCaseObject = new PublishingRequestCase();
        openingCaseObject.setOwner(userInstance.getUserIdentifier());
        openingCaseObject.setCustomerId(userInstance.getOrganizationUri());
        openingCaseObject.setResourceIdentifier(publicationIdentifier);
        openingCaseObject.setStatus(PublishingRequestStatus.PENDING);
        return openingCaseObject;
    }

    public static PublishingRequestCase createQuery(UserInstance userInstance,
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
            .withPublisher(new Organization.Builder().withId(this.getCustomerId()).build())
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
        if (!(o instanceof PublishingRequestCase)) {
            return false;
        }
        PublishingRequestCase that = (PublishingRequestCase) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getResourceIdentifier(), that.getResourceIdentifier())
               && getStatus() == that.getStatus()
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getRowVersion(), that.getRowVersion());
    }

    public PublishingRequestCase approve() {
        var copy = copy();
        copy.setStatus(PublishingRequestStatus.APPROVED);
        return copy;
    }

    public PublishingRequestCase copy() {
        var copy = new PublishingRequestCase();
        copy.setIdentifier(this.getIdentifier());
        copy.setStatus(this.getStatus());
        copy.setModifiedDate(this.getModifiedDate());
        copy.setCreatedDate(this.getCreatedDate());
        copy.setRowVersion(this.getRowVersion());
        copy.setCustomerId(this.getCustomerId());
        copy.setResourceIdentifier(this.getResourceIdentifier());
        copy.setOwner(this.getOwner());
        return copy;
    }

    private static PublishingRequestCase createPublishingRequestIdentifyingObject(
        UserInstance userInstance,
        SortableIdentifier publicationIdentifier,
        SortableIdentifier publishingRequestIdentifier) {

        var newPublishingRequest = new PublishingRequestCase();
        newPublishingRequest.setOwner(userInstance.getUserIdentifier());
        newPublishingRequest.setCustomerId(userInstance.getOrganizationUri());
        newPublishingRequest.setResourceIdentifier(publicationIdentifier);
        newPublishingRequest.setIdentifier(publishingRequestIdentifier);
        return newPublishingRequest;
    }
}
