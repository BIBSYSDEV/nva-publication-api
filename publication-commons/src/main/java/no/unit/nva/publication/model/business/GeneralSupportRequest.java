package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.storage.GeneralSupportRequestDao;
import no.unit.nva.publication.model.storage.TicketDao;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(GeneralSupportRequest.TYPE)
public class GeneralSupportRequest extends TicketEntry {

    public static final String TYPE = "GeneralSupportRequest";

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
        generalSupportRequest.setReceivingOrganizationDetails(createDefaultReceivingOrganizationDetails(userInstance));
        return generalSupportRequest;
    }

    @Override
    public String getType() {
        return TYPE;
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
        copy.setReceivingOrganizationDetails(this.getReceivingOrganizationDetails());
        return copy;
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
