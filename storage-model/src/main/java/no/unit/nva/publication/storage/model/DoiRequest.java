package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequestStatus;

public class DoiRequest {

    public static final String TYPE = DoiRequest.class.getSimpleName();
    private final SortableIdentifier identifier;
    private final SortableIdentifier resourceIdentifier;
    private final DoiRequestStatus status;
    private final Instant modifiedDate;
    @JsonAlias("date")
    private final Instant createdDate;
    private final URI customerId;

    @JsonCreator
    public DoiRequest(@JsonProperty("identifier") SortableIdentifier identifier,
                      @JsonProperty("resourceIdentifier") SortableIdentifier resourceIdentifier,
                      @JsonProperty("customerId") URI customerId,
                      @JsonProperty("status") DoiRequestStatus status,
                      @JsonProperty("modifiedDate") Instant modifiedDate,
                      @JsonProperty("createdDate") Instant createdDate) {
        this.identifier = identifier;
        this.resourceIdentifier = resourceIdentifier;
        this.status = status;
        this.modifiedDate = modifiedDate;
        this.createdDate = createdDate;
        this.customerId = customerId;
    }

    public SortableIdentifier getIdentifier() {
        return identifier;
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

    public URI getCustomerId() {
        return customerId;
    }
}
