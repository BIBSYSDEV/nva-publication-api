package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.PublishingRequestCase.createOpeningCaseObject;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Clock;
import java.util.function.Supplier;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = DoiRequest.TYPE, value = DoiRequest.class),
    @JsonSubTypes.Type(name = PublishingRequestCase.TYPE, value = PublishingRequestCase.class)
})
public interface TicketEntry extends Entity {
    
    static <T extends TicketEntry> TicketEntry createNewTicket(Publication publication,
                                                               Class<T> ticketType,
                                                               Clock clock,
                                                               Supplier<SortableIdentifier> identifierProvider)
        throws ConflictException, NotFoundException {
        var newTicket = createNewTicketEntry(publication, ticketType, clock, identifierProvider);
        newTicket.validateRequirements(publication);
        return newTicket;
    }
    
    SortableIdentifier getResourceIdentifier();
    
    void validateRequirements(Publication publication) throws NotFoundException, ConflictException;
    
    private static <T extends TicketEntry> TicketEntry createNewTicketEntry(
        Publication publication,
        Class<T> ticketType,
        Clock clock,
        Supplier<SortableIdentifier> identifierProvider) {
        
        if (DoiRequest.class.equals(ticketType)) {
            return createNewDoiRequest(publication, clock, identifierProvider);
        }
        if (PublishingRequestCase.class.equals(ticketType)) {
            return createNewPublishingRequestEntry(publication, clock, identifierProvider);
        }
        throw new UnsupportedOperationException();
    }
    
    private static TicketEntry createNewDoiRequest(Publication publication,
                                                   Clock clock,
                                                   Supplier<SortableIdentifier> identifierProvider) {
        var doiRequest = DoiRequest.fromPublication(publication);
        setServiceControlledFields(doiRequest, clock, identifierProvider);
        return doiRequest;
    }
    
    private static void setServiceControlledFields(TicketEntry ticketEntry,
                                                   Clock clock,
                                                   Supplier<SortableIdentifier> identifierProvider) {
        var now = clock.instant();
        ticketEntry.setCreatedDate(now);
        ticketEntry.setModifiedDate(now);
        ticketEntry.setVersion(Entity.nextVersion());
        ticketEntry.setIdentifier(identifierProvider.get());
    }
    
    private static TicketEntry createNewPublishingRequestEntry(Publication publication,
                                                               Clock clock,
                                                               Supplier<SortableIdentifier> identifierProvider) {
        var userInstance = UserInstance.fromPublication(publication);
        var entry = createOpeningCaseObject(userInstance, publication.getIdentifier());
        setServiceControlledFields(entry, clock, identifierProvider);
        return entry;
    }
}
