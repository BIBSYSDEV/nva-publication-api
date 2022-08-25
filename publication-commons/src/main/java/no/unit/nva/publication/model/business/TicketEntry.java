package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.PublishingRequestCase.createOpeningCaseObject;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.time.Clock;
import java.util.function.Supplier;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;

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
        throws ConflictException {
        var newTicket = createNewTicketEntry(publication, ticketType, clock, identifierProvider);
        newTicket.validateCreationRequirements(publication);
        return newTicket;
    }
    
    static Stream<Class<? extends TicketEntry>> ticketTypes() {
        return Stream.of(DoiRequest.class, PublishingRequestCase.class);
    }
    
    static <T extends TicketEntry> TicketEntry requestNewTicket(Publication publication, Class<T> ticketType) {
        if (DoiRequest.class.equals(ticketType)) {
            return DoiRequest.fromPublication(publication);
        } else if (PublishingRequestCase.class.equals(ticketType)) {
            return createOpeningCaseObject(UserInstance.fromPublication(publication), publication.getIdentifier());
        }
        throw new RuntimeException("Unrecognized ticket type");
    }
    
    static <T extends TicketEntry> T createQueryObject(URI customerId,
                                                       SortableIdentifier resourceIdentifier,
                                                       Class<T> ticketType) {
        if (DoiRequest.class.equals(ticketType)) {
            return ticketType.cast(DoiRequest.builder()
                                       .withResourceIdentifier(resourceIdentifier)
                                       .withCustomerId(customerId)
                                       .build());
        }
        return ticketType.cast(PublishingRequestCase.createQueryObject(customerId, resourceIdentifier));
    }
    
    static <T extends TicketEntry> T createQueryObject(SortableIdentifier ticketIdentifier, Class<T> ticketType) {
        if (DoiRequest.class.equals(ticketType)) {
            return ticketType.cast(DoiRequest.builder().withIdentifier(ticketIdentifier).build());
        } else if (PublishingRequestCase.class.equals(ticketType)) {
            return ticketType.cast(PublishingRequestCase.createQueryObject(ticketIdentifier));
        } else {
            throw new RuntimeException("Unsupported ticket type");
        }
    }
    
    static TicketEntry createQueryObject(UserInstance userInstance,
                                         SortableIdentifier ticketIdentifier,
                                         Class<? extends TicketEntry> ticketType) {
        if (DoiRequest.class.equals(ticketType)) {
            return DoiRequest.createQueryObject(userInstance, ticketIdentifier);
        } else if (PublishingRequestCase.class.equals(ticketType)) {
            return PublishingRequestCase.createQueryObject(userInstance, ticketIdentifier);
        } else {
            throw new RuntimeException("Unsupported ticket type");
        }
    }
    
    SortableIdentifier getResourceIdentifier();
    
    void validateCreationRequirements(Publication publication) throws ConflictException;
    
    void validateCompletionRequirements(Publication publication);
    
    default TicketEntry updateStatus(Publication publication, TicketStatus ticketStatus)
        throws ConflictException, BadRequestException {
        switch (ticketStatus) {
            case COMPLETED:
                return complete(publication);
            case CLOSED:
                return close(publication);
            case PENDING:
                return reopen(publication);
            default:
                throw new BadRequestException("Unknown status");
        }
    }
    
    default TicketEntry complete(Publication publication) {
        var updated = this.copy();
        updated.setStatus(TicketStatus.COMPLETED);
        updated.validateCompletionRequirements(publication);
        return updated;
    }
    
    default TicketEntry close(Publication publication) throws ConflictException {
        var updated = this.copy();
        updated.setStatus(TicketStatus.CLOSED);
        updated.validateClosingRequirements(publication);
        return updated;
    }
    
    void validateClosingRequirements(Publication publication) throws ConflictException;
    
    default TicketEntry reopen(Publication publication) throws ConflictException {
        var updated = this.copy();
        updated.setStatus(TicketStatus.PENDING);
        updated.validateReopeningRequirements(publication);
        return updated;
    }
    
    void validateReopeningRequirements(Publication publication) throws ConflictException;
    
    TicketEntry copy();
    
    TicketStatus getStatus();
    
    void setStatus(TicketStatus ticketStatus);
    
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
    
    final class Constants {
        
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
