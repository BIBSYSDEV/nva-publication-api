package no.unit.nva.publication.storage.model;

import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public class ApprovePublicationRequestBuilder {

    private final ApprovePublicationRequest approvePublicationRequest;

    protected ApprovePublicationRequestBuilder() {
        this.approvePublicationRequest = new ApprovePublicationRequest();
    }

    public ApprovePublicationRequestBuilder withIdentifier(SortableIdentifier identifier) {
        approvePublicationRequest.setIdentifier(identifier);
        return this;
    }

    public ApprovePublicationRequestBuilder withResourceIdentifier(SortableIdentifier resourceIdentifier) {
        approvePublicationRequest.setResourceIdentifier(resourceIdentifier);
        return this;
    }

    public ApprovePublicationRequestBuilder withStatus(ApprovePublicationRequestStatus status) {
        approvePublicationRequest.setStatus(status);
        return this;
    }

    public ApprovePublicationRequestBuilder withModifiedDate(Instant modifiedDate) {
        approvePublicationRequest.setModifiedDate(modifiedDate);
        return this;
    }

    public ApprovePublicationRequestBuilder withCreatedDate(Instant createdDate) {
        approvePublicationRequest.setCreatedDate(createdDate);
        return this;
    }

    public ApprovePublicationRequestBuilder withCustomerId(URI customerId) {
        approvePublicationRequest.setCustomerId(customerId);
        return this;
    }

    public ApprovePublicationRequestBuilder withOwner(String owner) {
        approvePublicationRequest.setOwner(owner);
        return this;
    }

    public ApprovePublicationRequestBuilder withResourceStatus(PublicationStatus resourceStatus) {
        approvePublicationRequest.setResourceStatus(resourceStatus);
        return this;
    }

    public ApprovePublicationRequestBuilder withResourceTitle(String resourceTitle) {
        approvePublicationRequest.setResourceTitle(resourceTitle);
        return this;
    }

    public ApprovePublicationRequestBuilder withContributors(List<Contributor> contributors) {
        this.approvePublicationRequest.setContributors(contributors);
        return this;
    }

    public ApprovePublicationRequest build() {
        return this.approvePublicationRequest;
    }

    public ApprovePublicationRequestBuilder withRowVersion(String rowVersion) {
        approvePublicationRequest.setRowVersion(rowVersion);
        return this;
    }
}
