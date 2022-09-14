package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.PublishingRequestCase.createOpeningCaseObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = DoiRequest.TYPE, value = DoiRequest.class),
    @JsonSubTypes.Type(name = PublishingRequestCase.TYPE, value = PublishingRequestCase.class),
    @JsonSubTypes.Type(name = GeneralSupportRequest.TYPE, value = GeneralSupportRequest.class)
})
public abstract class TicketEntry implements Entity {
    
    public static final String SEEN_BY_OWNER_FIELD = "seenByOwner";
    
    @JsonProperty(SEEN_BY_OWNER_FIELD)
    private Boolean seenByOwner;
    
    protected TicketEntry() {
    }
    
    public void persistUpdate(TicketService ticketService) {
        ticketService.updateTicket(this);
    }
    
    public abstract SortableIdentifier getResourceIdentifier();
    
    public abstract void validateCreationRequirements(Publication publication) throws ConflictException;
    
    public abstract void validateCompletionRequirements(Publication publication);
    
    public TicketEntry complete(Publication publication) {
        var updated = this.copy();
        updated.setStatus(TicketStatus.COMPLETED);
        updated.validateCompletionRequirements(publication);
        updated.setModifiedDate(Instant.now());
        return updated;
    }
    
    public final TicketEntry close() throws ApiGatewayException {
        validateClosingRequirements();
        var updated = this.copy();
        updated.setStatus(TicketStatus.CLOSED);
        updated.setModifiedDate(Instant.now());
        return updated;
    }
    
    public void validateClosingRequirements() throws ApiGatewayException {
        if (!getStatus().equals(TicketStatus.PENDING)) {
            var errorMessage =
                String.format("Cannot close a ticket that has any other status than %s", TicketStatus.PENDING);
            throw new BadRequestException(errorMessage);
        }
    }
    
    public abstract TicketEntry copy();
    
    public abstract TicketStatus getStatus();
    
    public abstract void setStatus(TicketStatus ticketStatus);
    
    public final List<Message> fetchMessages(TicketService ticketService) {
        return ticketService.fetchTicketMessages(this);
    }
    
    public final TicketEntry refresh() {
        var refreshed = this.copy();
        refreshed.setModifiedDate(Instant.now());
        return refreshed;
    }
    
    public final TicketEntry persistNewTicket(TicketService ticketService) throws ApiGatewayException {
        // this is the only place that deprecated should be called.
        return ticketService.createTicket(this, this.getClass());
    }
    
    public final Boolean getSeenByOwner() {
        return seenByOwner;
    }
    
    public final void setSeenByOwner(Boolean seenByOwner) {
        this.seenByOwner = seenByOwner;
    }
    
    public final TicketEntry markUnseenByOwner() {
        seenByOwner = false;
        this.setModifiedDate(Instant.now());
        return this;
    }
    
    public static <T extends TicketEntry> TicketEntry createNewTicket(Publication publication,
                                                                      Class<T> ticketType,
                                                                      Supplier<SortableIdentifier> identifierProvider)
        throws ConflictException {
        var newTicket = createNewTicketEntry(publication, ticketType, identifierProvider);
        newTicket.validateCreationRequirements(publication);
        return newTicket;
    }
    
    public static <T extends TicketEntry> TicketEntry requestNewTicket(Publication publication, Class<T> ticketType) {
        if (DoiRequest.class.equals(ticketType)) {
            return DoiRequest.fromPublication(publication);
        } else if (PublishingRequestCase.class.equals(ticketType)) {
            return createOpeningCaseObject(UserInstance.fromPublication(publication), publication.getIdentifier());
        } else if (GeneralSupportRequest.class.equals(ticketType)) {
            return GeneralSupportRequest.fromPublication(publication);
        }
        throw new RuntimeException("Unrecognized ticket type");
    }
    
    public static <T extends TicketEntry> T createQueryObject(URI customerId,
                                                              SortableIdentifier resourceIdentifier,
                                                              Class<T> ticketType) {
        if (DoiRequest.class.equals(ticketType)) {
            return ticketType.cast(DoiRequest.builder()
                                       .withResourceIdentifier(resourceIdentifier)
                                       .withCustomerId(customerId)
                                       .build());
        }
        if (PublishingRequestCase.class.equals(ticketType)) {
            return ticketType.cast(PublishingRequestCase.createQueryObject(customerId, resourceIdentifier));
        }
        if (GeneralSupportRequest.class.equals(ticketType)) {
            return ticketType.cast(GeneralSupportRequest.createQueryObject(customerId, resourceIdentifier));
        }
        throw new UnsupportedOperationException();
    }
    
    public static UntypedTicketQueryObject createQueryObject(UserInstance userInstance,
                                                             SortableIdentifier ticketIdentifier) {
        return UntypedTicketQueryObject.create(userInstance, ticketIdentifier);
    }
    
    public static UntypedTicketQueryObject createQueryObject(SortableIdentifier ticketIdentifier) {
        return UntypedTicketQueryObject.create(ticketIdentifier);
    }
    
    public static TicketEntry createNewGeneralSupportRequest(Publication publication,
                                                             Supplier<SortableIdentifier> identifierProvider) {
        var ticket = GeneralSupportRequest.fromPublication(publication);
        setServiceControlledFields(ticket, identifierProvider);
        return ticket;
    }
    
    public TicketEntry fetch(TicketService ticketService) throws NotFoundException {
        return ticketService.fetchTicket(this);
    }
    
    private static <T extends TicketEntry> TicketEntry createNewTicketEntry(
        Publication publication,
        Class<T> ticketType,
        Supplier<SortableIdentifier> identifierProvider) {
        
        if (DoiRequest.class.equals(ticketType)) {
            return createNewDoiRequest(publication, identifierProvider);
        }
        if (PublishingRequestCase.class.equals(ticketType)) {
            return createNewPublishingRequestEntry(publication, identifierProvider);
        }
        if (GeneralSupportRequest.class.equals(ticketType)) {
            return createNewGeneralSupportRequest(publication, identifierProvider);
        }
        throw new UnsupportedOperationException();
    }
    
    private static TicketEntry createNewDoiRequest(Publication publication,
                                                   Supplier<SortableIdentifier> identifierProvider) {
        var doiRequest = DoiRequest.fromPublication(publication);
        setServiceControlledFields(doiRequest, identifierProvider);
        return doiRequest;
    }
    
    private static void setServiceControlledFields(TicketEntry ticketEntry,
                                                   Supplier<SortableIdentifier> identifierProvider) {
        var now = Instant.now();
        ticketEntry.setCreatedDate(now);
        ticketEntry.setModifiedDate(now);
        ticketEntry.setIdentifier(identifierProvider.get());
    }
    
    private static TicketEntry createNewPublishingRequestEntry(Publication publication,
                                                               Supplier<SortableIdentifier> identifierProvider) {
        var userInstance = UserInstance.fromPublication(publication);
        var entry = createOpeningCaseObject(userInstance, publication.getIdentifier());
        setServiceControlledFields(entry, identifierProvider);
        return entry;
    }
    
    public static final class Constants {
        
        public static final String STATUS_FIELD = "status";
        public static final String MODIFIED_DATE_FIELD = "modifiedDate";
        public static final String CREATED_DATE_FIELD = "createdDate";
        public static final String OWNER_FIELD = "owner";
        public static final String CUSTOMER_ID_FIELD = "customerId";
        public static final String RESOURCE_IDENTIFIER_FIELD = "resourceIdentifier";
        public static final String IDENTIFIER_FIELD = "identifier";
        
        private Constants() {
        
        }
    }
}
