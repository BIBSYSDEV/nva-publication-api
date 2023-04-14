package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.TicketEntry.Constants.CREATED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CUSTOMER_ID_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.IDENTIFIER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.MODIFIED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.OWNER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.STATUS_FIELD;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.GeneralSupportRequestDao;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(GeneralSupportRequest.TYPE)
public class GeneralSupportRequest extends TicketEntry {
    
    public static final String TYPE = "GeneralSupportRequest";
    @JsonProperty(IDENTIFIER_FIELD)
    private SortableIdentifier identifier;
    @JsonProperty(CREATED_DATE_FIELD)
    private Instant createdDate;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private Instant modifiedDate;
    @JsonProperty(OWNER_FIELD)
    private User owner;
    @JsonProperty(CUSTOMER_ID_FIELD)
    private URI customerId;
    @JsonProperty(STATUS_FIELD)
    private TicketStatus status;
    @JsonProperty("assignee")
    private Optional<User> assignee;
    
    public GeneralSupportRequest() {
        super();
    }
    
    public static TicketEntry fromPublication(Publication publication) {
        var ticket = new GeneralSupportRequest();
        ticket.setPublicationDetails(PublicationDetails.create(publication));
        ticket.setOwner(extractOwner(publication));
        ticket.setCustomerId(extractCustomerId(publication));
        ticket.setCreatedDate(Instant.now());
        ticket.setModifiedDate(Instant.now());
        ticket.setStatus(TicketStatus.PENDING);
        ticket.setIdentifier(SortableIdentifier.next());
        ticket.setPublicationDetails(PublicationDetails.create(publication));
        ticket.setViewedBy(ViewedBy.addAll(ticket.getOwner()));
        ticket.setAssignee(null);
        return ticket;
    }
    
    public static GeneralSupportRequest createQueryObject(URI customerId, SortableIdentifier resourceIdentifier) {
        var ticket = new GeneralSupportRequest();
        ticket.setCustomerId(customerId);
        ticket.setPublicationDetails(PublicationDetails.create(resourceIdentifier));
        return ticket;
    }
    
    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }
    
    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }
    
    @JacocoGenerated
    @Override
    public Publication toPublication(ResourceService resourceService) {
        return attempt(() -> resourceService.getPublicationByIdentifier(getPublicationDetails().getIdentifier()))
                   .orElseThrow();
    }
    
    @Override
    public String getType() {
        return TYPE;
    }
    
    @Override
    public Instant getCreatedDate() {
        return this.createdDate;
    }
    
    @Override
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }
    
    @Override
    public Instant getModifiedDate() {
        return this.modifiedDate;
    }
    
    @Override
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
    
    @Override
    public User getOwner() {
        return this.owner;
    }
    
    public void setOwner(User owner) {
        this.owner = owner;
    }
    
    @Override
    public URI getCustomerId() {
        return this.customerId;
    }
    
    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }
    
    @Override
    public Dao toDao() {
        return new GeneralSupportRequestDao(this);
    }
    
    @Override
    public String getStatusString() {
        return this.getStatus().toString();
    }
    
    @Override
    public void validateCreationRequirements(Publication publication) {
        //NO OP
    }
    
    @Override
    public void validateCompletionRequirements(Publication publication) {
        //NO OP
    }
    
    @Override
    public TicketEntry copy() {
        var copy = new GeneralSupportRequest();
        copy.setStatus(this.getStatus());
        copy.setModifiedDate(this.getModifiedDate());
        copy.setIdentifier(this.getIdentifier());
        copy.setType(this.getType());
        copy.setCreatedDate(this.getCreatedDate());
        copy.setCustomerId(this.getCustomerId());
        copy.setOwner(this.getOwner());
        copy.setPublicationDetails(this.getPublicationDetails());
        copy.setViewedBy(this.getViewedBy());
        copy.setAssignee(this.getAssignee());
        return copy;
    }
    
    @Override
    public TicketStatus getStatus() {
        return this.status;
    }
    
    @Override
    public void setStatus(TicketStatus ticketStatus) {
        this.status = ticketStatus;
    }

    @Override
    public Optional<User> getAssignee() {
        return assignee;
    }

    @Override
    public void setAssignee(Optional<User> assignee) {
        this.assignee = assignee;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getCreatedDate(), getModifiedDate(), getOwner(), getCustomerId(),
            extractPublicationIdentifier(), getStatus(), getAssignee());
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GeneralSupportRequest)) {
            return false;
        }
        GeneralSupportRequest that = (GeneralSupportRequest) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && Objects.equals(extractPublicationIdentifier(), that.extractPublicationIdentifier())
               && getStatus() == that.getStatus()
               && Objects.equals(getAssignee(), that.getAssignee());
    }
    
    private static URI extractCustomerId(Publication publication) {
        return Optional.of(publication).map(Publication::getPublisher).map(Organization::getId).orElse(null);
    }
    
    private static User extractOwner(Publication publication) {
        return Optional.of(publication).map(Publication::getResourceOwner)
                   .map(ResourceOwner::getOwner)
                   .map(User::new)
                   .orElse(null);
    }
}
