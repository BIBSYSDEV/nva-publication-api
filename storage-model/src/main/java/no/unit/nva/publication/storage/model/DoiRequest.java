package no.unit.nva.publication.storage.model;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequest.Builder;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
public final class DoiRequest implements WithIdentifier, RowLevelSecurity, WithStatus, ResourceUpdate {

    public static final String RESOURCE_STATUS_FIELD = "resourceStatus";
    public static final String STATUS_FIELD = "status";
    public static final String TYPE = DoiRequest.class.getSimpleName();

    public static final String MISSING_RESOURCE_REFERENCE_ERROR = "Resource identifier cannot be null or empty";

    @JsonProperty
    private final SortableIdentifier identifier;
    @JsonProperty
    private final SortableIdentifier resourceIdentifier;
    @JsonProperty(STATUS_FIELD)
    private final DoiRequestStatus status;
    @JsonProperty(RESOURCE_STATUS_FIELD)
    private final PublicationStatus resourceStatus;
    @JsonProperty
    private final Instant modifiedDate;
    @JsonProperty
    @JsonAlias("date")
    private final Instant createdDate;
    @JsonProperty("customerId")
    private final URI customerId;
    @JsonProperty("owner")
    private final String owner;
    @JsonProperty("resourceTitle")
    private final String resourceTitle;

    @JsonCreator
    private DoiRequest(@JsonProperty("identifier") SortableIdentifier identifier,
                       @JsonProperty("resourceIdentifier") SortableIdentifier resourceIdentifier,
                       @JsonProperty("resourceTitle") String resourceTitle,
                       @JsonProperty("owner") String owner,
                       @JsonProperty("customerId") URI customerId,
                       @JsonProperty(STATUS_FIELD) DoiRequestStatus status,
                       @JsonProperty(RESOURCE_STATUS_FIELD) PublicationStatus resourceStatus,
                       @JsonProperty("createdDate") Instant createdDate,
                       @JsonProperty("modifiedDate") Instant modifiedDate) {
        this.identifier = identifier;
        this.resourceTitle = resourceTitle;
        this.resourceIdentifier = resourceIdentifier;
        this.status = status;
        this.resourceStatus = resourceStatus;
        this.modifiedDate = modifiedDate;
        this.createdDate = createdDate;
        this.customerId = customerId;
        this.owner = owner;
    }

    public static DoiRequest unvalidatedEntry(SortableIdentifier identifier,
                                              SortableIdentifier resourceIdentifier,
                                              String mainTitle,
                                              String owner,
                                              URI publisherId,
                                              DoiRequestStatus doiRequestStatus,
                                              PublicationStatus status,
                                              Instant createdDate,
                                              Instant modifiedDate) {
        return new DoiRequest(
            identifier,
            resourceIdentifier,
            mainTitle,
            owner,
            publisherId,
            doiRequestStatus,
            status,
            createdDate,
            modifiedDate
        );
    }

    public static DoiRequest fromResource(Resource resource, Clock clock) {
        Instant now = clock.instant();
        return newEntry(SortableIdentifier.next(),
            resource.getIdentifier(),
            resource.getEntityDescription().getMainTitle(),
            resource.getOwner(),
            resource.getCustomerId(),
            DoiRequestStatus.REQUESTED,
            resource.getStatus(),
            now
        );
    }

    public static DoiRequest newEntry(SortableIdentifier identifier,
                                      SortableIdentifier resourceIdentifier,
                                      String mainTitle,
                                      String owner,
                                      URI id,
                                      DoiRequestStatus requested,
                                      PublicationStatus status,
                                      Instant createdDate) {

        DoiRequest doiRequest = unvalidatedEntry(
            identifier,
            resourceIdentifier,
            mainTitle,
            owner,
            id,
            requested,
            status,
            createdDate,
            createdDate
        );
        doiRequest.validate();
        return doiRequest;
    }

    public static String getType() {
        return DoiRequest.TYPE;
    }

    public DoiRequest update(Resource resource) {
        return new DoiRequest(
            this.getIdentifier(),
            this.getResourceIdentifier(),
            resource.getEntityDescription().getMainTitle(),
            resource.getOwner(),
            resource.getPublisher().getId(),
            this.getStatus(),
            this.getResourceStatus(),
            this.getCreatedDate(),
            resource.getModifiedDate()
        );
    }

    public PublicationStatus getResourceStatus() {
        return resourceStatus;
    }

    public String getResourceTitle() {
        return resourceTitle;
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        throw new UnsupportedOperationException();
    }

    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    public DoiRequestStatus getStatus() {
        return status;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    @Override
    public URI getCustomerId() {
        return customerId;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public String getStatusString() {
        return Objects.nonNull(getStatus()) ? getStatus().toString() : null;
    }

    @Override
    public Publication toPublication() {

        no.unit.nva.model.DoiRequest doiRequest = new Builder()
            .withStatus(getStatus())
            .withModifiedDate(getModifiedDate())
            .withCreatedDate(getCreatedDate())
            .build();

        EntityDescription entityDescription = new EntityDescription.Builder()
            .withMainTitle(getResourceTitle())
            .build();

        Organization customer = new Organization.Builder()
            .withId(getCustomerId())
            .build();

        return new
            Publication.Builder()
            .withIdentifier(getResourceIdentifier())
            .withStatus(getResourceStatus())
            .withEntityDescription(entityDescription)
            .withPublisher(customer)
            .withOwner(getOwner())
            .withDoiRequest(doiRequest)
            .build();
    }

    public void validate() {
        if (isNull(resourceIdentifier)) {
            throw new IllegalArgumentException(MISSING_RESOURCE_REFERENCE_ERROR);
        }
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getResourceIdentifier(), getStatus(), getResourceStatus(), getModifiedDate(),
            getCreatedDate(),
            getCustomerId(), getOwner(), getResourceTitle(), getIdentifier());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoiRequest)) {
            return false;
        }
        DoiRequest that = (DoiRequest) o;
        return Objects.equals(getResourceIdentifier(), that.getResourceIdentifier())
               && getStatus() == that.getStatus()
               && getResourceStatus() == that.getResourceStatus()
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getResourceTitle(), that.getResourceTitle())
               && Objects.equals(getIdentifier(), that.getIdentifier());
    }
}
