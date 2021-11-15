package no.unit.nva.expansion.model;

import static java.util.Collections.emptySet;
import static java.util.Objects.nonNull;
import static no.unit.nva.expansion.model.ExpandedDoiRequest.TYPE;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.WithOrganizationScope;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.storage.model.DoiRequest;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(TYPE)
@SuppressWarnings("PMD.TooManyFields")
public final class ExpandedDoiRequest implements WithOrganizationScope, ExpandedDataEntry {

    public static final String TYPE = "DoiRequest";

    @JsonProperty
    private SortableIdentifier identifier;
    @JsonProperty
    private SortableIdentifier resourceIdentifier;
    @JsonProperty()
    private DoiRequestStatus status;
    @JsonProperty()
    private PublicationStatus resourceStatus;
    @JsonProperty
    private Instant modifiedDate;
    @JsonProperty
    @JsonAlias("date")
    private Instant createdDate;
    @JsonProperty
    private URI customerId;
    @JsonProperty
    private String owner;

    private String resourceTitle;
    @JsonProperty
    private Instant resourceModifiedDate;
    @JsonProperty
    private PublicationInstance<? extends Pages> resourcePublicationInstance;
    @JsonProperty
    private PublicationDate resourcePublicationDate;
    @JsonProperty
    private String resourcePublicationYear;
    @JsonProperty
    private URI doi;
    @JsonProperty
    private List<Contributor> contributors;
    @JsonProperty
    private Set<URI> organizationIds;

    public static ExpandedDoiRequest create(DoiRequest doiRequest,
                                            ResourceExpansionService resourceExpansionService) {
        ExpandedDoiRequest expandedDoiRequest = ExpandedDoiRequest.fromDoiRequest(doiRequest);
        Set<URI> ids = resourceExpansionService.getOrganizationIds(expandedDoiRequest.getOwner());
        expandedDoiRequest.setOrganizationIds(ids);
        return expandedDoiRequest;
    }

    @JacocoGenerated
    public List<Contributor> getContributors() {
        return contributors;
    }

    @JacocoGenerated
    public void setContributors(List<Contributor> contributors) {
        this.contributors = contributors;
    }

    @JacocoGenerated
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @JacocoGenerated
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    @JacocoGenerated
    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    @JacocoGenerated
    public void setResourceIdentifier(SortableIdentifier resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    @JacocoGenerated
    public DoiRequestStatus getStatus() {
        return status;
    }

    @JacocoGenerated
    public void setStatus(DoiRequestStatus status) {
        this.status = status;
    }

    @JacocoGenerated
    public PublicationStatus getResourceStatus() {
        return resourceStatus;
    }

    @JacocoGenerated
    public void setResourceStatus(PublicationStatus resourceStatus) {
        this.resourceStatus = resourceStatus;
    }

    @JacocoGenerated
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @JacocoGenerated
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @JacocoGenerated
    public Instant getCreatedDate() {
        return createdDate;
    }

    @JacocoGenerated
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    @JacocoGenerated
    public URI getCustomerId() {
        return customerId;
    }

    @JacocoGenerated
    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    @JacocoGenerated
    public String getOwner() {
        return owner;
    }

    @JacocoGenerated
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @JacocoGenerated
    public String getResourceTitle() {
        return resourceTitle;
    }

    @JacocoGenerated
    public void setResourceTitle(String resourceTitle) {
        this.resourceTitle = resourceTitle;
    }

    @JacocoGenerated
    public Instant getResourceModifiedDate() {
        return resourceModifiedDate;
    }

    @JacocoGenerated
    public void setResourceModifiedDate(Instant resourceModifiedDate) {
        this.resourceModifiedDate = resourceModifiedDate;
    }

    @JacocoGenerated
    public PublicationInstance<? extends Pages> getResourcePublicationInstance() {
        return resourcePublicationInstance;
    }

    @JacocoGenerated
    public void setResourcePublicationInstance(
        PublicationInstance<? extends Pages> resourcePublicationInstance) {
        this.resourcePublicationInstance = resourcePublicationInstance;
    }

    @JacocoGenerated
    public PublicationDate getResourcePublicationDate() {
        return resourcePublicationDate;
    }

    @JacocoGenerated
    public void setResourcePublicationDate(PublicationDate resourcePublicationDate) {
        this.resourcePublicationDate = resourcePublicationDate;
    }

    @JacocoGenerated
    public String getResourcePublicationYear() {
        return resourcePublicationYear;
    }

    @JacocoGenerated
    public void setResourcePublicationYear(String resourcePublicationYear) {
        this.resourcePublicationYear = resourcePublicationYear;
    }

    @JacocoGenerated
    public URI getDoi() {
        return doi;
    }

    @JacocoGenerated
    public void setDoi(URI doi) {
        this.doi = doi;
    }

    @Override
    public Set<URI> getOrganizationIds() {
        return nonNull(organizationIds) ? organizationIds : emptySet();
    }

    @Override
    public void setOrganizationIds(Set<URI> organizationIds) {
        this.organizationIds = organizationIds;
    }

    public DoiRequest toDoiRequest() {
        DoiRequest doiRequest = new DoiRequest();
        doiRequest.setDoi(this.getDoi());
        doiRequest.setContributors(this.getContributors());
        doiRequest.setCreatedDate(this.getCreatedDate());
        doiRequest.setIdentifier(this.getIdentifier());
        doiRequest.setCustomerId(this.getCustomerId());
        doiRequest.setModifiedDate(this.getModifiedDate());
        doiRequest.setOwner(this.getOwner());
        doiRequest.setResourceIdentifier(this.getResourceIdentifier());
        doiRequest.setResourceModifiedDate(this.getResourceModifiedDate());
        doiRequest.setResourcePublicationDate(this.getResourcePublicationDate());
        doiRequest.setResourcePublicationInstance(this.getResourcePublicationInstance());
        doiRequest.setResourcePublicationYear(this.getResourcePublicationYear());
        doiRequest.setResourceStatus(this.getResourceStatus());
        doiRequest.setResourceTitle(this.getResourceTitle());
        doiRequest.setStatus(this.getStatus());
        return doiRequest;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getResourceIdentifier(), getStatus(), getResourceStatus(),
                            getModifiedDate(),
                            getCreatedDate(), getCustomerId(), getOwner(), getResourceTitle(),
                            getResourceModifiedDate(),
                            getResourcePublicationInstance(), getResourcePublicationDate(),
                            getResourcePublicationYear(),
                            getDoi(), getContributors(), getOrganizationIds());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExpandedDoiRequest)) {
            return false;
        }
        ExpandedDoiRequest that = (ExpandedDoiRequest) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getResourceIdentifier(), that.getResourceIdentifier())
               && getStatus() == that.getStatus()
               && getResourceStatus() == that.getResourceStatus()
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getResourceTitle(), that.getResourceTitle())
               && Objects.equals(getResourceModifiedDate(), that.getResourceModifiedDate())
               && Objects.equals(getResourcePublicationInstance(), that.getResourcePublicationInstance())
               && Objects.equals(getResourcePublicationDate(), that.getResourcePublicationDate())
               && Objects.equals(getResourcePublicationYear(), that.getResourcePublicationYear())
               && Objects.equals(getDoi(), that.getDoi())
               && Objects.equals(getContributors(), that.getContributors())
               && Objects.equals(getOrganizationIds(), that.getOrganizationIds());
    }

    @Override
    public SortableIdentifier retrieveIdentifier() {
        return getIdentifier();
    }

    // should not become public. An ExpandedDoiRequest needs an Expansion service to be complete
    private static ExpandedDoiRequest fromDoiRequest(DoiRequest doiRequest) {
        ExpandedDoiRequest request = new ExpandedDoiRequest();
        request.setDoi(doiRequest.getDoi());
        request.setContributors(doiRequest.getContributors());
        request.setCreatedDate(doiRequest.getCreatedDate());
        request.setIdentifier(doiRequest.getIdentifier());
        request.setCustomerId(doiRequest.getCustomerId());
        request.setModifiedDate(doiRequest.getModifiedDate());
        request.setOwner(doiRequest.getOwner());
        request.setResourceIdentifier(doiRequest.getResourceIdentifier());
        request.setResourceModifiedDate(doiRequest.getResourceModifiedDate());
        request.setResourcePublicationDate(doiRequest.getResourcePublicationDate());
        request.setResourcePublicationInstance(doiRequest.getResourcePublicationInstance());
        request.setResourcePublicationYear(doiRequest.getResourcePublicationYear());
        request.setResourceStatus(doiRequest.getResourceStatus());
        request.setResourceTitle(doiRequest.getResourceTitle());
        request.setStatus(doiRequest.getStatus());
        return request;
    }
}
