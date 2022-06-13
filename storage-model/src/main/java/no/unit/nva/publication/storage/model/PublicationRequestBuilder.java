package no.unit.nva.publication.storage.model;

import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.PublicationStatus;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public class PublicationRequestBuilder {

    private final PublicationRequest publicationRequest;

    protected PublicationRequestBuilder() {
        this.publicationRequest = new PublicationRequest();
    }

    public PublicationRequestBuilder withIdentifier(SortableIdentifier identifier) {
        publicationRequest.setIdentifier(identifier);
        return this;
    }

    public PublicationRequestBuilder withResourceIdentifier(SortableIdentifier resourceIdentifier) {
        publicationRequest.setResourceIdentifier(resourceIdentifier);
        return this;
    }

    public PublicationRequestBuilder withStatus(PublicationRequestStatus status) {
        publicationRequest.setStatus(status);
        return this;
    }

    public PublicationRequestBuilder withModifiedDate(Instant modifiedDate) {
        publicationRequest.setModifiedDate(modifiedDate);
        return this;
    }

    public PublicationRequestBuilder withCreatedDate(Instant createdDate) {
        publicationRequest.setCreatedDate(createdDate);
        return this;
    }

    public PublicationRequestBuilder withCustomerId(URI customerId) {
        publicationRequest.setCustomerId(customerId);
        return this;
    }

    public PublicationRequestBuilder withOwner(String owner) {
        publicationRequest.setOwner(owner);
        return this;
    }

    public PublicationRequestBuilder withResourceStatus(PublicationStatus resourceStatus) {
        publicationRequest.setResourceStatus(resourceStatus);
        return this;
    }

    public PublicationRequestBuilder withResourceTitle(String resourceTitle) {
        publicationRequest.setResourceTitle(resourceTitle);
        return this;
    }

    public PublicationRequestBuilder withContributors(List<Contributor> contributors) {
        this.publicationRequest.setContributors(contributors);
        return this;
    }

    public PublicationRequest build() {
        return this.publicationRequest;
    }

    public PublicationRequestBuilder withRowVersion(String rowVersion) {
        publicationRequest.setRowVersion(rowVersion);
        return this;
    }
}
