package no.unit.nva.publication.storage.model;

import static java.util.Objects.nonNull;
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
import no.unit.nva.model.DoiRequestStatus;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
public class DoiRequest implements WithIdentifier, RowLevelSecurity, WithStatus {

    public static final String TYPE = DoiRequest.class.getSimpleName();
    public static final String MISSING_RESOURCE_REFERENCE_ERROR = "Resource identifier cannot be null or empty";
    @JsonProperty
    private final SortableIdentifier resourceIdentifier;
    @JsonProperty
    private final DoiRequestStatus status;
    @JsonProperty
    private final Instant modifiedDate;
    @JsonProperty
    @JsonAlias("date")
    private final Instant createdDate;
    @JsonProperty("customerId")
    private final URI customerId;
    @JsonAlias("date")
    private final String owner;
    @JsonProperty
    private SortableIdentifier identifier;

    @JsonCreator
    public DoiRequest(@JsonProperty("identifier") SortableIdentifier identifier,
                      @JsonProperty("resourceIdentifier") SortableIdentifier resourceIdentifier,
                      @JsonProperty("owner") String owner,
                      @JsonProperty("customerId") URI customerId,
                      @JsonProperty("status") String status,
                      @JsonProperty("modifiedDate") Instant modifiedDate,
                      @JsonProperty("createdDate") Instant createdDate) {
        this.identifier = identifier;
        this.resourceIdentifier = validateResourceIdentifier(resourceIdentifier);
        this.status = parseStatus(status);
        this.modifiedDate = modifiedDate;
        this.createdDate = createdDate;
        this.customerId = customerId;
        this.owner = owner;
    }

    public static DoiRequest fromResource(Resource resource, Clock clock) {
        Instant now = clock.instant();
        return new DoiRequest(SortableIdentifier.next(),
            resource.getIdentifier(),
            resource.getOwner(),
            resource.getCustomerId(),
            DoiRequestStatus.REQUESTED.toString(),
            now,
            now
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getResourceIdentifier(), getStatus(), getModifiedDate(), getCreatedDate(),
            getCustomerId(), getOwner());
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
        return
            Objects.equals(getIdentifier(), that.getIdentifier())
            && Objects.equals(getResourceIdentifier(), that.getResourceIdentifier())
            && getStatus().equals(that.getStatus())
            && Objects.equals(getModifiedDate(), that.getModifiedDate())
            && Objects.equals(getCreatedDate(), that.getCreatedDate())
            && Objects.equals(getCustomerId(), that.getCustomerId())
            && Objects.equals(getOwner(), that.getOwner());
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    @Override
    public String getStatus() {
        return status.toString();
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

    private DoiRequestStatus parseStatus(String status) {
        return DoiRequestStatus.parse(status);
    }

    private SortableIdentifier validateResourceIdentifier(SortableIdentifier resourceIdentifier) {
        if (nonNull(resourceIdentifier)) {
            return resourceIdentifier;
        }
        throw new IllegalArgumentException(MISSING_RESOURCE_REFERENCE_ERROR);
    }
}
