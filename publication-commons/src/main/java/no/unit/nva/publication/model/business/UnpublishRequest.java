package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.model.storage.UnpublishRequestDao;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class UnpublishRequest extends TicketEntry {

    public static final String RESOURCE_STATUS_FIELD = "resourceStatus";
    public static final String TYPE = "UnpublishRequest";

    @JsonProperty(RESOURCE_STATUS_FIELD)
    private PublicationStatus resourceStatus;

    public UnpublishRequest() {
        super();
    }

    public static TicketEntry fromPublication(Publication publication) {

        // TODO: Who is ticket owner? Currently publication owner to pass tests
        var owner = extractOwner(publication);

        return UnpublishRequest.builder()
                   .withIdentifier(SortableIdentifier.next())
                   .withResourceIdentifier(publication.getIdentifier())
                   .withStatus(TicketStatus.PENDING)
                   .withResourceStatus(publication.getStatus())
                   .withModifiedDate(Instant.now())
                   .withCreatedDate(Instant.now())
                   .withCustomerId(extractCustomerId(publication))
                   .withOwner(owner)
                   .withViewedBy(ViewedBy.addAll(owner)) // TODO: Correct?
                   .withAssignee(null) // TODO: Add no one?
                   .build();
    }

    public static UnpublishRequest createQueryObject(URI customerId, SortableIdentifier resourceIdentifier) {
        var ticket = new UnpublishRequest();
        ticket.setResourceIdentifier(resourceIdentifier);
        ticket.setCustomerId(customerId);
        return ticket;
    }

    public static UnpublishRequest.Builder builder() {
        return new UnpublishRequest.Builder();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public TicketDao toDao() {
        return new UnpublishRequestDao(this);
    }

    @Override
    public void validateCreationRequirements(Publication publication) {
        // TODO:
    }

    @Override
    public void validateCompletionRequirements(Publication publication) {
        // TODO:
    }

    @Override
    public TicketEntry copy() {
        return UnpublishRequest.builder()
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

    public PublicationStatus getResourceStatus() {
        return resourceStatus;
    }

    public void setResourceStatus(PublicationStatus resourceStatus) {
        this.resourceStatus = resourceStatus;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getCreatedDate(), getModifiedDate(), getOwner(), getCustomerId(),
                            getResourceIdentifier(), getStatus(), getAssignee(), getOwnerAffiliation());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnpublishRequest)) {
            return false;
        }
        UnpublishRequest that = (UnpublishRequest) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && getStatus() == that.getStatus()
               && Objects.equals(getAssignee(), that.getAssignee())
               && Objects.equals(getOwnerAffiliation(), that.getOwnerAffiliation());
    }

    private static URI extractCustomerId(Publication publication) {
        return Optional.of(publication).map(Publication::getPublisher).map(Organization::getId).orElse(null);
    }

    private static User extractOwner(Publication publication) {
        return Optional.of(publication).map(Publication::getResourceOwner)
                   .map(ResourceOwner::getOwner)
                   .map(owner -> new User(owner.getValue()))
                   .orElse(null);
    }

    public static final class Builder {
        private final UnpublishRequest unpublishRequest;

        private Builder() {
            unpublishRequest = new UnpublishRequest();
        }

        public UnpublishRequest.Builder withIdentifier(SortableIdentifier identifier) {
            unpublishRequest.setIdentifier(identifier);
            return this;
        }

        public UnpublishRequest.Builder withStatus(TicketStatus status) {
            unpublishRequest.setStatus(status);
            return this;
        }

        public UnpublishRequest.Builder withAssignee(Username assignee) {
            unpublishRequest.setAssignee(assignee);
            return this;
        }

        public UnpublishRequest.Builder withResourceStatus(PublicationStatus resourceStatus) {
            unpublishRequest.setResourceStatus(resourceStatus);
            return this;
        }

        public UnpublishRequest.Builder withModifiedDate(Instant modifiedDate) {
            unpublishRequest.setModifiedDate(modifiedDate);
            return this;
        }

        public UnpublishRequest.Builder withCreatedDate(Instant createdDate) {
            unpublishRequest.setCreatedDate(createdDate);
            return this;
        }

        public UnpublishRequest.Builder withCustomerId(URI customerId) {
            unpublishRequest.setCustomerId(customerId);
            return this;
        }

        public UnpublishRequest.Builder withOwner(User owner) {
            unpublishRequest.setOwner(owner);
            return this;
        }

        public UnpublishRequest.Builder withOwnerAffiliation(URI ownerAffiliation) {
            unpublishRequest.setOwnerAffiliation(ownerAffiliation);
            return this;
        }

        public UnpublishRequest.Builder withResponsibilityArea(URI responsibilityArea) {
            unpublishRequest.setResponsibilityArea(responsibilityArea);
            return this;
        }

        public UnpublishRequest.Builder withReceivingOrganizationDetails(ReceivingOrganizationDetails receivingOrganizationDetails) {
            unpublishRequest.setReceivingOrganizationDetails(receivingOrganizationDetails);
            return this;
        }

        public UnpublishRequest.Builder withViewedBy(Set<User> viewedBy) {
            unpublishRequest.setViewedBy(viewedBy);
            return this;
        }

        public UnpublishRequest.Builder withResourceIdentifier(SortableIdentifier resourceIdentifier) {
            unpublishRequest.setResourceIdentifier(resourceIdentifier);
            return this;
        }

        public Builder withFinalizedBy(Username finalizedBy) {
            unpublishRequest.setFinalizedBy(finalizedBy);
            return this;
        }

        public Builder withFinalizedDate(Instant finalizedDate) {
            unpublishRequest.setFinalizedDate(finalizedDate);
            return this;
        }

        public UnpublishRequest build() {
            return unpublishRequest;
        }
    }
}
