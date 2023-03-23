package no.unit.nva.publication.model.business;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.PublishingRequestCase.createOpeningCaseObject;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.PUBLICATION_DETAILS_FIELD;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;

@SuppressWarnings("PMD.GodClass")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(name = DoiRequest.TYPE, value = DoiRequest.class),
    @JsonSubTypes.Type(name = PublishingRequestCase.TYPE, value = PublishingRequestCase.class),
    @JsonSubTypes.Type(name = GeneralSupportRequest.TYPE, value = GeneralSupportRequest.class)})
public abstract class TicketEntry implements Entity {

    public static final User SUPPORT_SERVICE_CORRESPONDENT = new User("SupportService");

    public static final String VIEWED_BY_FIELD = "viewedBy";
    public static final String TICKET_WITHOUT_REFERENCE_TO_PUBLICATION_ERROR =
        "Ticket without reference to publication";
    public static final String DOI_REQUEST_EXCEPTION_MESSAGE_WHEN_NON_PUBLISHED =
        "Can not create DoiRequest ticket for unpublished publication, use draft doi flow instead.";
    @JsonProperty(VIEWED_BY_FIELD)
    private ViewedBy viewedBy;
    @JsonProperty(PUBLICATION_DETAILS_FIELD)
    private PublicationDetails publicationDetails;
    @JsonProperty("resourceIdentifier")
    private SortableIdentifier resourceIdentifier;

    protected TicketEntry() {
        viewedBy = ViewedBy.empty();
    }

    public static <T extends TicketEntry> TicketEntry createNewTicket(Publication publication, Class<T> ticketType,
                                                                      Supplier<SortableIdentifier> identifierProvider)
        throws ConflictException {
        var newTicket = createNewTicketEntry(publication, ticketType, identifierProvider);
        newTicket.validateCreationRequirements(publication);
        return newTicket;
    }

    public static <T extends TicketEntry> TicketEntry requestNewTicket(Publication publication, Class<T> ticketType)
        throws BadRequestException {
        if (DoiRequest.class.equals(ticketType)) {
            return requestDoiRequestTicket(publication);
        } else if (PublishingRequestCase.class.equals(ticketType)) {
            return createOpeningCaseObject(publication);
        } else if (GeneralSupportRequest.class.equals(ticketType)) {
            return GeneralSupportRequest.fromPublication(publication);
        }
        throw new RuntimeException("Unrecognized ticket type");
    }

    private static DoiRequest requestDoiRequestTicket(Publication publication) throws BadRequestException {
        if(isPublished(publication)) {
            return DoiRequest.fromPublication(publication);
        } else {
            throw new BadRequestException(DOI_REQUEST_EXCEPTION_MESSAGE_WHEN_NON_PUBLISHED);
        }
    }

    private static boolean isPublished(Publication publication) {
        return PublicationStatus.PUBLISHED.equals(publication.getStatus());
    }

    public static <T extends TicketEntry> T createQueryObject(URI customerId, SortableIdentifier resourceIdentifier,
                                                              Class<T> ticketType) {
        if (DoiRequest.class.equals(ticketType)) {
            return ticketType.cast(DoiRequest.builder()
                                       .withPublicationDetails(PublicationDetails.create(resourceIdentifier))
                                       .withCustomerId(customerId)
                                       .build());
        }
        if (PublishingRequestCase.class.equals(ticketType)) {
            return ticketType.cast(PublishingRequestCase.createQueryObject(resourceIdentifier, customerId));
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

    //TODO: Delete resourceIdentifier field ASAP.
    public SortableIdentifier getResourceIdentifier() {
        return Optional.ofNullable(resourceIdentifier)
                   .or(() -> Optional.ofNullable(getPublicationDetails()).map(PublicationDetails::getIdentifier))
                   .orElse(null);
    }

    public void setResourceIdentifier(SortableIdentifier resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    public Set<User> getViewedBy() {
        return nonNull(viewedBy) ? viewedBy : Collections.emptySet();
    }

    public void setViewedBy(Set<User> viewedBy) {
        this.viewedBy = new ViewedBy(viewedBy);
    }

    public void persistUpdate(TicketService ticketService) {
        ticketService.updateTicket(this);
    }

    public final SortableIdentifier extractPublicationIdentifier() {
        return Optional.ofNullable(getPublicationDetails())
                   .map(PublicationDetails::getIdentifier)
                   .or(() -> Optional.ofNullable(getResourceIdentifier()))
                   .orElseThrow(() -> new IllegalStateException(TICKET_WITHOUT_REFERENCE_TO_PUBLICATION_ERROR));
    }

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
            var errorMessage = String.format("Cannot close a ticket that has any other status than %s",
                TicketStatus.PENDING);
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

    public final TicketEntry markUnreadByOwner() {
        viewedBy.remove(this.getOwner());
        return this;
    }

    public TicketEntry fetch(TicketService ticketService) throws NotFoundException {
        return ticketService.fetchTicket(this);
    }

    public TicketEntry markReadByOwner() {
        viewedBy.add(getOwner());
        return this;
    }

    public TicketEntry markReadForCurators() {
        viewedBy.add(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT);
        return this;
    }

    public TicketEntry markUnreadForCurators() {
        viewedBy.remove(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT);
        return this;
    }

    public final String extractPublicationTitle() {
        return Optional.of(this.getPublicationDetails()).map(PublicationDetails::getTitle).orElse(null);
    }

    public PublicationDetails getPublicationDetails() {
        return publicationDetails;
    }

    public void setPublicationDetails(PublicationDetails publicationDetails) {
        this.publicationDetails = publicationDetails;
    }

    public TicketEntry update(Resource resource) {
        this.getPublicationDetails().update(resource);
        this.setPublicationDetails(
            this.getPublicationDetails().update(resource));
        return this;
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
        var entry = createOpeningCaseObject(publication);
        setServiceControlledFields(entry, identifierProvider);
        return entry;
    }

    public static final class Constants {

        public static final String STATUS_FIELD = "status";
        public static final String MODIFIED_DATE_FIELD = "modifiedDate";
        public static final String CREATED_DATE_FIELD = "createdDate";
        public static final String OWNER_FIELD = "owner";
        public static final String CUSTOMER_ID_FIELD = "customerId";
        public static final String PUBLICATION_DETAILS_FIELD = "publicationDetails";
        public static final String IDENTIFIER_FIELD = "identifier";

        private Constants() {

        }
    }
}
