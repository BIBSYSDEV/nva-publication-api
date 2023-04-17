package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.TicketEntry.Constants.CREATED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CUSTOMER_ID_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.IDENTIFIER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.MODIFIED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.OWNER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.STATUS_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.WORKFLOW;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.exception.InvalidPublicationException;
import no.unit.nva.publication.model.storage.PublishingRequestDao;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(PublishingRequestCase.TYPE)
//TODO fix God class
@SuppressWarnings("PMD.GodClass")
public class PublishingRequestCase extends TicketEntry {
    
    public static final String RESOURCE_LACKS_REQUIRED_DATA = "Resource does not have required data to be "
                                                              + "published: ";
    
    public static final String TYPE = "PublishingRequestCase";
    public static final String MARKED_FOR_DELETION_ERROR =
        "Publication is marked for deletion and cannot be published.";
    
    @JsonProperty(IDENTIFIER_FIELD)
    private SortableIdentifier identifier;
    @JsonProperty(STATUS_FIELD)
    private TicketStatus status;
    @JsonProperty(CUSTOMER_ID_FIELD)
    private URI customerId;
    @JsonProperty(OWNER_FIELD)
    private User owner;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private Instant modifiedDate;
    @JsonProperty(CREATED_DATE_FIELD)
    private Instant createdDate;

    @JsonProperty(WORKFLOW)
    private PublishingWorkflow workflow;
    
    public PublishingRequestCase() {
        super();
    }

    public static PublishingRequestCase createOpeningCaseObject(Publication publication,
                                                                PublishingWorkflow publishingWorkflow) {
        var userInstance = UserInstance.fromPublication(publication);
        var openingCaseObject = new PublishingRequestCase();
        openingCaseObject.setOwner(userInstance.getUser());
        openingCaseObject.setCustomerId(userInstance.getOrganizationUri());
        openingCaseObject.setStatus(TicketStatus.PENDING);
        openingCaseObject.setViewedBy(ViewedBy.addAll(openingCaseObject.getOwner()));
        openingCaseObject.setPublicationDetails(PublicationDetails.create(publication));
        openingCaseObject.setWorkflow(publishingWorkflow);
        return openingCaseObject;
    }

    public static PublishingRequestCase createOpeningCaseObject(Publication publication) {
        return createOpeningCaseObject(publication, null);
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
        queryObject.setPublicationDetails(PublicationDetails.create(resourceIdentifier));
        queryObject.setCustomerId(customerId);
        return queryObject;
    }
    
    public static void assertThatPublicationHasMinimumMandatoryFields(Publication resource)
        throws InvalidPublicationException {

        if (!resource.isPublishable()) {
            throwErrorWhenPublishingResourceThatDoesNotHaveRequiredData(resource);
        }
    }

    public PublishingWorkflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(PublishingWorkflow workflow) {
        this.workflow = workflow;
    }

    @Override
    public void validateCreationRequirements(Publication publication)
        throws ConflictException {
        if (PublicationStatus.DRAFT_FOR_DELETION == publication.getStatus()) {
            throw new ConflictException(MARKED_FOR_DELETION_ERROR);
        }
        assertThatPublicationHasMinimumMandatoryFields(publication);
    }
    
    @Override
    public void validateCompletionRequirements(Publication publication) {
    }
    
    @Override
    public PublishingRequestCase complete(Publication publication) {
        return (PublishingRequestCase) super.complete(publication);
    }
    
    @Override
    public PublishingRequestCase copy() {
        var copy = new PublishingRequestCase();
        copy.setIdentifier(this.getIdentifier());
        copy.setStatus(this.getStatus());
        copy.setModifiedDate(this.getModifiedDate());
        copy.setCreatedDate(this.getCreatedDate());
        copy.setCustomerId(this.getCustomerId());
        copy.setOwner(this.getOwner());
        copy.setViewedBy(this.getViewedBy());
        copy.setPublicationDetails(this.getPublicationDetails());
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
        return attempt(() -> resourceService.getPublicationByIdentifier(extractPublicationIdentifier()))
                   .orElseThrow();
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
    public User getOwner() {
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
    
    public void setOwner(User owner) {
        this.owner = owner;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getStatus(), getCustomerId(), getOwner(), getModifiedDate(),
            getCreatedDate());
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
        newPublishingRequest.setOwner(userInstance.getUser());
        newPublishingRequest.setCustomerId(userInstance.getOrganizationUri());
        newPublishingRequest.setPublicationDetails(PublicationDetails.create(publicationIdentifier));
        newPublishingRequest.setIdentifier(publishingRequestIdentifier);
        return newPublishingRequest;
    }

    private static void throwErrorWhenPublishingResourceThatDoesNotHaveRequiredData(Publication resource)
        throws InvalidPublicationException {
        throw new InvalidPublicationException(RESOURCE_LACKS_REQUIRED_DATA + resource.getIdentifier().toString());
    }
}
