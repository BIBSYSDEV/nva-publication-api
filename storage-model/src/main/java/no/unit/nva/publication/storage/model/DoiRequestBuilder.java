package no.unit.nva.publication.storage.model;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;

public class DoiRequestBuilder {

    private final DoiRequest doiRequest;

    protected DoiRequestBuilder() {
        this.doiRequest = new DoiRequest();
    }

    public DoiRequestBuilder withIdentifier(SortableIdentifier identifier) {
        doiRequest.setIdentifier(identifier);
        return this;
    }

    public DoiRequestBuilder withResourceIdentifier(SortableIdentifier resourceIdentifier) {
        doiRequest.setResourceIdentifier(resourceIdentifier);
        return this;
    }

    public DoiRequestBuilder withStatus(DoiRequestStatus status) {
        doiRequest.setStatus(status);
        return this;
    }

    public DoiRequestBuilder withResourceStatus(PublicationStatus resourceStatus) {
        doiRequest.setResourceStatus(resourceStatus);
        return this;
    }

    public DoiRequestBuilder withModifiedDate(Instant modifiedDate) {
        doiRequest.setModifiedDate(modifiedDate);
        return this;
    }

    public DoiRequestBuilder withCreatedDate(Instant createdDate) {
        doiRequest.setCreatedDate(createdDate);
        return this;
    }

    public DoiRequestBuilder withCustomerId(URI customerId) {
        doiRequest.setCustomerId(customerId);
        return this;
    }

    public DoiRequestBuilder withOwner(String owner) {
        doiRequest.setOwner(owner);
        return this;
    }

    public DoiRequestBuilder withResourceTitle(String resourceTitle) {
        doiRequest.setResourceTitle(resourceTitle);
        return this;
    }

    public DoiRequestBuilder withResourceModifiedDate(Instant resourceModifiedDate) {
        doiRequest.setResourceModifiedDate(resourceModifiedDate);
        return this;
    }

    public DoiRequestBuilder withResourcePublicationDate(PublicationDate resourcePublicationDate) {
        doiRequest.setResourcePublicationDate(resourcePublicationDate);
        return this;
    }

    public DoiRequestBuilder withResourcePublicationYear(String resourcePublicationYear) {
        doiRequest.setResourcePublicationYear(resourcePublicationYear);
        return this;
    }

    public DoiRequestBuilder withResourcePublicationInstance(
        PublicationInstance<? extends Pages> resourcePublicationInstance) {
        this.doiRequest.setResourcePublicationInstance(resourcePublicationInstance);
        return this;
    }

    public DoiRequestBuilder withContributors(List<Contributor> contributors) {
        this.doiRequest.setContributors(contributors);
        return this;
    }

    public DoiRequestBuilder withDoi(URI doi) {
        doiRequest.setDoi(doi);
        return this;
    }

    public DoiRequest build() {
        return this.doiRequest;
    }
}
