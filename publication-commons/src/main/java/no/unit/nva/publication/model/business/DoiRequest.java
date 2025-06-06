package no.unit.nva.publication.model.business;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.publicationstate.DoiAssignedEvent;
import no.unit.nva.publication.model.business.publicationstate.DoiRejectedEvent;
import no.unit.nva.publication.model.business.publicationstate.DoiRequestedEvent;
import no.unit.nva.publication.model.business.publicationstate.TicketEvent;
import no.unit.nva.publication.model.storage.DoiRequestDao;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.storage.model.exceptions.IllegalDoiRequestUpdate;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount", "PMD.TooManyFields"})
public class DoiRequest extends TicketEntry {

    public static final String RESOURCE_STATUS_FIELD = "resourceStatus";
    public static final String TYPE = "DoiRequest";

    public static final String RESOURCE_IDENTIFIER_MISMATCH_ERROR = "Resource identifier mismatch";
    public static final String WRONG_PUBLICATION_STATUS_ERROR =
        "DoiRequests may only be created for publications with statuses %s";
    public static final Set<PublicationStatus> ACCEPTABLE_PUBLICATION_STATUSES =
        Set.of(PublicationStatus.PUBLISHED,
               PublicationStatus.PUBLISHED_METADATA,
               PublicationStatus.DRAFT);
    public static final String DOI_REQUEST_APPROVAL_FAILURE = "Cannot approve DoiRequest for non-published publication";
    @JsonProperty(RESOURCE_STATUS_FIELD)
    private PublicationStatus resourceStatus;
    @JsonProperty("ticketEvent")
    private TicketEvent ticketEvent;

    public DoiRequest() {
        super();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DoiRequest create(Resource resource, UserInstance userInstance) {
        var doiRequest = extractDataFromResource(resource);
        doiRequest.setIdentifier(SortableIdentifier.next());
        doiRequest.setStatus(TicketStatus.PENDING);
        doiRequest.setViewedBy(Collections.emptySet());
        var now = Clock.systemDefaultZone().instant();
        doiRequest.setModifiedDate(now);
        doiRequest.setCreatedDate(now);
        doiRequest.setTicketEvent(DoiRequestedEvent.create(userInstance, now));
        doiRequest.setOwnerAffiliation(userInstance.getTopLevelOrgCristinId());
        doiRequest.setResponsibilityArea(userInstance.getPersonAffiliation());
        doiRequest.setReceivingOrganizationDetails(createDefaultReceivingOrganizationDetails(userInstance));
        doiRequest.setOwner(userInstance.getUser());
        return doiRequest;
    }

    @Override
    public String getType() {
        return DoiRequest.TYPE;
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
    public DoiRequest complete(Publication publication, UserInstance userInstance) {
        var completed = (DoiRequest) super.complete(publication, userInstance);
        completed.setTicketEvent(DoiAssignedEvent.create(userInstance, Instant.now()));
        return completed;
    }

    @Override
    public DoiRequest close(UserInstance userInstance) throws ApiGatewayException {
        var closed = (DoiRequest) super.close(userInstance);
        closed.setTicketEvent(DoiRejectedEvent.create(userInstance, Instant.now()));
        return closed;
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
                   .withOwnerAffiliation(getOwnerAffiliation())
                   .withResponsibilityArea(getResponsibilityArea())
                   .withReceivingOrganizationDetails(getReceivingOrganizationDetails())
                   .withFinalizedBy(getFinalizedBy())
                   .withFinalizedDate(getFinalizedDate())
                   .build();
    }

    @Override
    public TicketDao toDao() {
        return new DoiRequestDao(this);
    }

    public TicketEvent getTicketEvent() {
        return ticketEvent;
    }

    public void setTicketEvent(TicketEvent ticketEvent) {
        this.ticketEvent = ticketEvent;
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

    private void setResourceStatus(PublicationStatus resourceStatus) {
        this.resourceStatus = resourceStatus;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getStatus(), getResourceStatus(), getModifiedDate(), getCreatedDate(),
                            getCustomerId(), getOwner(), getAssignee(), getOwnerAffiliation());
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
               && Objects.equals(getAssignee(), that.getAssignee())
               && Objects.equals(getOwnerAffiliation(), that.getOwnerAffiliation());
    }

    public boolean hasTicketEvent() {
        return nonNull(getTicketEvent());
    }

    private static DoiRequest extractDataFromResource(DoiRequest doiRequest, Resource resource) {
        var copy = doiRequest.copy();
        copy.setResourceIdentifier(resource.getIdentifier());
        copy.setOwner(resource.getResourceOwner().getUser());
        copy.setCustomerId(resource.getCustomerId());
        copy.setResourceStatus(resource.getStatus());
        return copy;
    }

    private static DoiRequest extractDataFromResource(Resource resource) {
        return extractDataFromResource(new DoiRequest(), resource);
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

        public Builder withOwnerAffiliation(URI ownerAffiliation) {
            doiRequest.setOwnerAffiliation(ownerAffiliation);
            return this;
        }

        public Builder withResponsibilityArea(URI responsibilityArea) {
            doiRequest.setResponsibilityArea(responsibilityArea);
            return this;
        }

        public Builder withReceivingOrganizationDetails(ReceivingOrganizationDetails receivingOrganizationDetails) {
            doiRequest.setReceivingOrganizationDetails(receivingOrganizationDetails);
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

        public Builder withResourceIdentifier(SortableIdentifier resourceIdentifier) {
            doiRequest.setResourceIdentifier(resourceIdentifier);
            return this;
        }

        public Builder withFinalizedBy(Username finalizedBy) {
            doiRequest.setFinalizedBy(finalizedBy);
            return this;
        }

        public Builder withFinalizedDate(Instant finalizedDate) {
            doiRequest.setFinalizedDate(finalizedDate);
            return this;
        }

        public DoiRequest build() {
            return doiRequest;
        }
    }
}
