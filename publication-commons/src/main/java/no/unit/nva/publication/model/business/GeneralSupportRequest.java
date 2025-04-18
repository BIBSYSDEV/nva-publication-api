package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.TicketEntry.Constants.ASSIGNEE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CREATED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CUSTOMER_ID_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.IDENTIFIER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.MODIFIED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.OWNER_AFFILIATION_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.STATUS_FIELD;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.storage.GeneralSupportRequestDao;
import no.unit.nva.publication.model.storage.TicketDao;
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
    @JsonProperty(CUSTOMER_ID_FIELD)
    private URI customerId;
    @JsonProperty(STATUS_FIELD)
    private TicketStatus status;
    @JsonProperty(ASSIGNEE_FIELD)
    private Username assignee;
    @JsonProperty(OWNER_AFFILIATION_FIELD)
    private URI ownerAffiliation;

    public GeneralSupportRequest() {
        super();
    }

    public static GeneralSupportRequest createQueryObject(URI customerId, SortableIdentifier resourceIdentifier) {
        var ticket = new GeneralSupportRequest();
        ticket.setResourceIdentifier(resourceIdentifier);
        ticket.setCustomerId(customerId);
        return ticket;
    }

    public static GeneralSupportRequest create(Resource resource, UserInstance userInstance) {
        var generalSupportRequest = new GeneralSupportRequest();
        generalSupportRequest.setResourceIdentifier(resource.getIdentifier());
        generalSupportRequest.setCustomerId(resource.getCustomerId());
        generalSupportRequest.setCreatedDate(Instant.now());
        generalSupportRequest.setModifiedDate(Instant.now());
        generalSupportRequest.setStatus(TicketStatus.PENDING);
        generalSupportRequest.setIdentifier(SortableIdentifier.next());
        generalSupportRequest.setViewedBy(Collections.emptySet());
        generalSupportRequest.setOwnerAffiliation(userInstance.getTopLevelOrgCristinId());
        generalSupportRequest.setResponsibilityArea(userInstance.getPersonAffiliation());
        generalSupportRequest.setOwner(userInstance.getUser());
        return generalSupportRequest;
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
        return attempt(() -> resourceService.getPublicationByIdentifier(getResourceIdentifier()))
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
    public URI getCustomerId() {
        return this.customerId;
    }

    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    @Override
    public TicketDao toDao() {
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
        copy.setResourceIdentifier(this.getResourceIdentifier());
        copy.setType(this.getType());
        copy.setCreatedDate(this.getCreatedDate());
        copy.setCustomerId(this.getCustomerId());
        copy.setOwner(this.getOwner());
        copy.setViewedBy(this.getViewedBy());
        copy.setAssignee(this.getAssignee());
        copy.setOwnerAffiliation(this.getOwnerAffiliation());
        copy.setFinalizedBy(this.getFinalizedBy());
        copy.setFinalizedDate(this.getFinalizedDate());
        copy.setResponsibilityArea(this.getResponsibilityArea());
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
    public Username getAssignee() {
        return assignee;
    }

    @Override
    public void setAssignee(Username assignee) {
        this.assignee = assignee;
    }

    @Override
    public URI getOwnerAffiliation() {
        return ownerAffiliation;
    }

    @Override
    public void setOwnerAffiliation(URI ownerAffiliation) {
        this.ownerAffiliation = ownerAffiliation;
    }

    @Override
    public void validateAssigneeRequirements(Publication publication) {
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
        if (!(o instanceof GeneralSupportRequest)) {
            return false;
        }
        GeneralSupportRequest that = (GeneralSupportRequest) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && getStatus() == that.getStatus()
               && Objects.equals(getAssignee(), that.getAssignee())
               && Objects.equals(getOwnerAffiliation(), that.getOwnerAffiliation());
    }
}
