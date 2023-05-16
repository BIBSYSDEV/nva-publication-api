package no.unit.nva.publication.model.business;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.DoiRequestUtils.extractDataFromResource;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.ASSIGNEE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CREATED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CUSTOMER_ID_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.IDENTIFIER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.OWNER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.STATUS_FIELD;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.storage.DoiRequestDao;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.exceptions.IllegalDoiRequestUpdate;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;

@JsonTypeInfo(use = Id.NAME, property = "type")
@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount", "PMD.TooManyFields"})
public class DoiRequest extends TicketEntry {

    public static final String RESOURCE_STATUS_FIELD = "resourceStatus";
    public static final String TYPE = "DoiRequest";

    public static final String RESOURCE_IDENTIFIER_MISMATCH_ERROR = "Resource identifier mismatch";
    public static final String WRONG_PUBLICATION_STATUS_ERROR =
        "DoiRequests may only be created for publications with statuses %s";
    public static final Set<PublicationStatus> ACCEPTABLE_PUBLICATION_STATUSES = Set.of(PublicationStatus.PUBLISHED,
                                                                                        PublicationStatus.PUBLISHED_METADATA,
                                                                                        PublicationStatus.DRAFT);
    public static final String DOI_REQUEST_APPROVAL_FAILURE = "Cannot approve DoiRequest for non-published publication";
    @JsonProperty(IDENTIFIER_FIELD)
    private SortableIdentifier identifier;
    @JsonProperty(STATUS_FIELD)
    private TicketStatus status;
    @JsonProperty(RESOURCE_STATUS_FIELD)
    private PublicationStatus resourceStatus;
    @JsonProperty
    private Instant modifiedDate;
    @JsonProperty(CREATED_DATE_FIELD)
    @JsonAlias("date")
    private Instant createdDate;
    @JsonProperty(CUSTOMER_ID_FIELD)
    private URI customerId;
    @JsonProperty(OWNER_FIELD)
    private User owner;
    @JsonProperty(ASSIGNEE_FIELD)
    private Username assignee;

    public DoiRequest() {
        super();
    }

    public static DoiRequest fromPublication(Publication publication) {
        return newDoiRequestForResource(Resource.fromPublication(publication));
    }

    public static DoiRequest newDoiRequestForResource(Resource resource) {
        return newDoiRequestForResource(SortableIdentifier.next(), resource, Clock.systemDefaultZone().instant());
    }

    public static DoiRequest newDoiRequestForResource(Resource resource, Instant now) {

        var doiRequest = extractDataFromResource(resource);
        doiRequest.setIdentifier(SortableIdentifier.next());
        doiRequest.setStatus(TicketStatus.PENDING);
        doiRequest.setModifiedDate(now);
        doiRequest.setCreatedDate(now);
        doiRequest.setViewedBy(ViewedBy.addAll(doiRequest.getOwner()));
        return doiRequest;
    }

    public static DoiRequest newDoiRequestForResource(SortableIdentifier doiRequestIdentifier,
                                                      Resource resource,
                                                      Instant now) {

        var doiRequest = extractDataFromResource(resource);
        doiRequest.setIdentifier(doiRequestIdentifier);
        doiRequest.setStatus(TicketStatus.PENDING);
        doiRequest.setModifiedDate(now);
        doiRequest.setCreatedDate(now);
        doiRequest.validate();
        doiRequest.setViewedBy(ViewedBy.addAll(doiRequest.getOwner()));
        return doiRequest;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public Publication toPublication(ResourceService resourceService) {
        return Optional.ofNullable(getResourceIdentifier())
                   .map(attempt(resourceService::getPublicationByIdentifier))
                   .map(Try::orElseThrow)
                   .orElse(null);
    }

    @Override
    public String getType() {
        return DoiRequest.TYPE;
    }

    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }

    @Override
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @Override
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @Override
    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    @Override
    public URI getCustomerId() {
        return customerId;
    }

    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    @Override
    public TicketDao toDao() {
        return new DoiRequestDao(this);
    }

    @Override
    public String getStatusString() {
        return nonNull(getStatus()) ? getStatus().toString() : null;
    }

    @Override
    public void validateCreationRequirements(Publication publication) throws ConflictException {
        if (publicationDoesNotHaveAnExpectedStatus(publication)) {
            throw new ConflictException(String.format(WRONG_PUBLICATION_STATUS_ERROR, ACCEPTABLE_PUBLICATION_STATUSES));
        }
    }

    @JacocoGenerated
    @Override
    public void validateCompletionRequirements(Publication publication) {
        if (!publication.satisfiesFindableDoiRequirements()) {
            throw new InvalidTicketStatusTransitionException(DOI_REQUEST_APPROVAL_FAILURE);
        }
    }

    @Override
    public DoiRequest complete(Publication publication, Username finalizedBy) {
        return (DoiRequest) super.complete(publication, finalizedBy);
    }

    @Override
    public DoiRequest copy() {
        return DoiRequest.builder()
                   .withIdentifier(getIdentifier())
                   .withResourceIdentifier(getResourceIdentifier())
                   .withStatus(getStatus())
                   .withResourceStatus(getResourceStatus())
                   .withModifiedDate(getModifiedDate())
                   .withCreatedDate(getCreatedDate())
                   .withCustomerId(getCustomerId())
                   .withOwner(getOwner())
                   .withViewedBy(this.getViewedBy())
                   .withAssignee(getAssignee())
                   .build();
    }

    @Override
    public TicketStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    @Override
    public Username getAssignee() {
        return assignee;
    }

    @Override
    public void setAssignee(Username assignee) {
        this.assignee = assignee;
    }

    @Override
    public void validateAssigneeRequirements(Publication publication) {
    }

    public DoiRequest update(Resource resource) {
        if (updateIsAboutTheSameResource(resource)) {
            return extractDataFromResource(this, resource);
        }
        throw new IllegalDoiRequestUpdate(RESOURCE_IDENTIFIER_MISMATCH_ERROR);
    }

    public PublicationStatus getResourceStatus() {
        return resourceStatus;
    }

    public void setResourceStatus(PublicationStatus resourceStatus) {
        this.resourceStatus = resourceStatus;
    }

    public void validate() {
        attempt(this::getResourceIdentifier)
            .toOptional()
            .orElseThrow(() -> new IllegalStateException(TICKET_WITHOUT_REFERENCE_TO_PUBLICATION_ERROR));
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getStatus(), getResourceStatus(), getModifiedDate(), getCreatedDate(),
                            getCustomerId(), getOwner(), getAssignee());
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
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && getStatus() == that.getStatus()
               && getResourceStatus() == that.getResourceStatus()
               && Objects.equals(getResourceIdentifier(), that.getResourceIdentifier())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getAssignee(), that.getAssignee());
    }

    private boolean publicationDoesNotHaveAnExpectedStatus(Publication publication) {
        return !ACCEPTABLE_PUBLICATION_STATUSES.contains(publication.getStatus());
    }

    private boolean updateIsAboutTheSameResource(Resource resource) {
        return resource.getIdentifier().equals(this.getResourceIdentifier());
    }

    public static final class Builder {

        private final DoiRequest doiRequest;

        private Builder() {
            doiRequest = new DoiRequest();
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            doiRequest.setIdentifier(identifier);
            return this;
        }

        public Builder withStatus(TicketStatus status) {
            doiRequest.setStatus(status);
            return this;
        }

        public Builder withAssignee(Username assignee) {
            doiRequest.setAssignee(assignee);
            return this;
        }

        public Builder withResourceStatus(PublicationStatus resourceStatus) {
            doiRequest.setResourceStatus(resourceStatus);
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            doiRequest.setModifiedDate(modifiedDate);
            return this;
        }

        public Builder withCreatedDate(Instant createdDate) {
            doiRequest.setCreatedDate(createdDate);
            return this;
        }

        public Builder withCustomerId(URI customerId) {
            doiRequest.setCustomerId(customerId);
            return this;
        }

        public Builder withOwner(User owner) {
            doiRequest.setOwner(owner);
            return this;
        }

        public Builder withViewedBy(Set<User> viewedBy) {
            doiRequest.setViewedBy(viewedBy);
            return this;
        }

        public DoiRequest build() {
            return doiRequest;
        }

        public Builder withResourceIdentifier(SortableIdentifier resourceIdentifier) {
            doiRequest.setResourceIdentifier(resourceIdentifier);
            return this;
        }
    }
}
