package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.TicketEntry.Constants.CREATED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CUSTOMER_ID_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.IDENTIFIER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.MODIFIED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.OWNER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.RESOURCE_IDENTIFIER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.STATUS_FIELD;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.model.storage.PublishingRequestDao;
import no.unit.nva.publication.model.storage.TicketDao;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(PublishingRequestCase.TYPE)
public class PublishingRequestCase implements TicketEntry {
    
    public static final String TYPE = "PublishingRequestCase";
    
    public static final String ALREADY_PUBLISHED_ERROR =
        "Publication is already published.";
    public static final String MARKED_FOR_DELETION_ERROR =
        "Publication is marked for deletion and cannot be published.";
    
    @JsonProperty(IDENTIFIER_FIELD)
    private SortableIdentifier identifier;
    @JsonProperty(RESOURCE_IDENTIFIER_FIELD)
    private SortableIdentifier resourceIdentifier;
    @JsonProperty(STATUS_FIELD)
    private TicketStatus status;
    @JsonProperty(CUSTOMER_ID_FIELD)
    private URI customerId;
    @JsonProperty(OWNER_FIELD)
    private String owner;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private Instant modifiedDate;
    @JsonProperty(CREATED_DATE_FIELD)
    private Instant createdDate;
    
    private UUID version;
    
    public PublishingRequestCase() {
    }
    
    public static PublishingRequestCase createOpeningCaseObject(UserInstance userInstance,
                                                                SortableIdentifier publicationIdentifier) {
    
        var openingCaseObject = new PublishingRequestCase();
        openingCaseObject.setOwner(userInstance.getUserIdentifier());
        openingCaseObject.setCustomerId(userInstance.getOrganizationUri());
        openingCaseObject.setResourceIdentifier(publicationIdentifier);
        openingCaseObject.setStatus(TicketStatus.PENDING);
        return openingCaseObject;
    }
    
    public static PublishingRequestCase createQueryObject(UserInstance userInstance,
                                                          SortableIdentifier publicationIdentifier,
                                                          SortableIdentifier publishingRequestIdentifier) {
        return createPublishingRequestIdentifyingObject(userInstance,
            publicationIdentifier,
            publishingRequestIdentifier);
    }
    
    public static PublishingRequestCase createQueryObject(SortableIdentifier resourceIdentifier, URI customerId) {
        var queryObject = new PublishingRequestCase();
        queryObject.setResourceIdentifier(resourceIdentifier);
        queryObject.setCustomerId(customerId);
        return queryObject;
    }
    
    public static PublishingRequestCase createQueryObject(SortableIdentifier ticketIdentifier) {
        var queryObject = new PublishingRequestCase();
        queryObject.setIdentifier(ticketIdentifier);
        return queryObject;
    }
    
    public static PublishingRequestCase createQueryObject(URI customerId, SortableIdentifier resourceIdentifier) {
        var queryObject = new PublishingRequestCase();
        queryObject.setCustomerId(customerId);
        queryObject.setResourceIdentifier(resourceIdentifier);
        return queryObject;
    }
    
    public static PublishingRequestCase createQueryObject(UserInstance userInstance,
                                                          SortableIdentifier ticketIdentifier) {
        var queryObject = new PublishingRequestCase();
        queryObject.setOwner(userInstance.getUserIdentifier());
        queryObject.setCustomerId(userInstance.getOrganizationUri());
        queryObject.setIdentifier(ticketIdentifier);
        return queryObject;
    }
    
    @Override
    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }
    
    @Override
    public void validateCreationRequirements(Publication publication) throws ConflictException {
        if (PublicationStatus.PUBLISHED == publication.getStatus()) {
            throw new ConflictException(ALREADY_PUBLISHED_ERROR);
        }
        if (PublicationStatus.DRAFT_FOR_DELETION == publication.getStatus()) {
            throw new ConflictException(MARKED_FOR_DELETION_ERROR);
        }
    }
    
    @Override
    public void validateCompletionRequirements(Publication publication) {
    }
    
    @Override
    public PublishingRequestCase complete(Publication publication) {
        return (PublishingRequestCase) TicketEntry.super.complete(publication);
    }
    
    @Override
    public PublishingRequestCase copy() {
        var copy = new PublishingRequestCase();
        copy.setIdentifier(this.getIdentifier());
        copy.setStatus(this.getStatus());
        copy.setModifiedDate(this.getModifiedDate());
        copy.setCreatedDate(this.getCreatedDate());
        copy.setVersion(this.getVersion());
        copy.setCustomerId(this.getCustomerId());
        copy.setResourceIdentifier(this.getResourceIdentifier());
        copy.setOwner(this.getOwner());
        return copy;
    }
    
    @Override
    public TicketStatus getStatus() {
        return status;
    }
    
    @Override
    public void setStatus(TicketStatus status) {
        this.status = status;
    }
    
    public void setResourceIdentifier(SortableIdentifier resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
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
    public Publication toPublication() {
    
        return new Publication.Builder()
                   .withIdentifier(getResourceIdentifier())
                   .withResourceOwner(new ResourceOwner(getOwner(), null))
                   .withPublisher(new Organization.Builder().withId(this.getCustomerId()).build())
                   .build();
    }
    
    @Override
    public UUID getVersion() {
        return version;
    }
    
    @Override
    public void setVersion(UUID version) {
        this.version = version;
    }
    
    @Override
    public String getType() {
        return PublishingRequestCase.TYPE;
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
    public String getOwner() {
        return owner;
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
        return new PublishingRequestDao(this);
    }
    
    @Override
    public String getStatusString() {
        return status.toString();
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getResourceIdentifier(), getStatus(), getCustomerId(), getOwner(),
            getModifiedDate(), getCreatedDate());
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublishingRequestCase)) {
            return false;
        }
        PublishingRequestCase that = (PublishingRequestCase) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getResourceIdentifier(), that.getResourceIdentifier())
               && getStatus() == that.getStatus()
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getCreatedDate(), that.getCreatedDate());
    }
    
    private static PublishingRequestCase createPublishingRequestIdentifyingObject(
        UserInstance userInstance,
        SortableIdentifier publicationIdentifier,
        SortableIdentifier publishingRequestIdentifier) {
        
        var newPublishingRequest = new PublishingRequestCase();
        newPublishingRequest.setOwner(userInstance.getUserIdentifier());
        newPublishingRequest.setCustomerId(userInstance.getOrganizationUri());
        newPublishingRequest.setResourceIdentifier(publicationIdentifier);
        newPublishingRequest.setIdentifier(publishingRequestIdentifier);
        return newPublishingRequest;
    }
}
