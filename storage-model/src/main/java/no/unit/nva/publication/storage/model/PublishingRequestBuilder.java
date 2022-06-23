package no.unit.nva.publication.storage.model;

import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.PublicationStatus;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public class PublishingRequestBuilder {

    private final PublishingRequest publishingRequest;

    protected PublishingRequestBuilder() {
        this.publishingRequest = new PublishingRequest();
    }

    public PublishingRequestBuilder withIdentifier(SortableIdentifier identifier) {
        publishingRequest.setIdentifier(identifier);
        return this;
    }

    public PublishingRequestBuilder withResourceIdentifier(SortableIdentifier resourceIdentifier) {
        publishingRequest.setResourceIdentifier(resourceIdentifier);
        return this;
    }

    public PublishingRequestBuilder withStatus(PublishingRequestStatus status) {
        publishingRequest.setStatus(status);
        return this;
    }

    public PublishingRequestBuilder withModifiedDate(Instant modifiedDate) {
        publishingRequest.setModifiedDate(modifiedDate);
        return this;
    }

    public PublishingRequestBuilder withCreatedDate(Instant createdDate) {
        publishingRequest.setCreatedDate(createdDate);
        return this;
    }

    public PublishingRequestBuilder withCustomerId(URI customerId) {
        publishingRequest.setCustomerId(customerId);
        return this;
    }

    public PublishingRequestBuilder withOwner(String owner) {
        publishingRequest.setOwner(owner);
        return this;
    }

    public PublishingRequestBuilder withResourceStatus(PublicationStatus resourceStatus) {
        publishingRequest.setResourceStatus(resourceStatus);
        return this;
    }

    public PublishingRequestBuilder withResourceTitle(String resourceTitle) {
        publishingRequest.setResourceTitle(resourceTitle);
        return this;
    }

    public PublishingRequestBuilder withContributors(List<Contributor> contributors) {
        this.publishingRequest.setContributors(contributors);
        return this;
    }

    public PublishingRequest build() {
        return this.publishingRequest;
    }

    public PublishingRequestBuilder withRowVersion(String rowVersion) {
        publishingRequest.setRowVersion(rowVersion);
        return this;
    }
}
