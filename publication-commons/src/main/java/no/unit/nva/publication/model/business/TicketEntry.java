package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.publication.model.business.PublishingRequestCase.createOpeningCaseObject;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.PUBLICATION_DETAILS_FIELD;
import static nva.commons.core.attempt.Try.attempt;

@SuppressWarnings({"PMD.GodClass", "PMD.FinalizeOverloaded"})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(name = DoiRequest.TYPE, value = DoiRequest.class),
    @JsonSubTypes.Type(name = PublishingRequestCase.TYPE, value = PublishingRequestCase.class),
    @JsonSubTypes.Type(name = GeneralSupportRequest.TYPE, value = GeneralSupportRequest.class)})
public abstract class TicketEntry implements Entity {

    public static final String DOI_REQUEST_EXCEPTION_MESSAGE_WHEN_NON_PUBLISHED =
        "Can not create DoiRequest ticket for unpublished publication, use draft doi flow instead.";
    private static final String VIEWED_BY_FIELD = "viewedBy";
    public static final String TICKET_WITHOUT_REFERENCE_TO_PUBLICATION_ERROR =
        "Ticket without reference to publication";
    private static final Set<PublicationStatus> PUBLISHED_STATUSES = Set.of(PUBLISHED, PUBLISHED_METADATA);
    private static final String FINALIZED_BY = "finalizedBy";
    private static final String FINALIZED_DATE = "finalizedDate";
    private static final String RESOURCE_IDENTIFIER = "resourceIdentifier";
    @JsonProperty(VIEWED_BY_FIELD)
    private ViewedBy viewedBy;
    @JsonProperty(PUBLICATION_DETAILS_FIELD)
    private PublicationDetails publicationDetails;
    @JsonProperty(RESOURCE_IDENTIFIER)
    private SortableIdentifier resourceIdentifier;
    @JsonProperty(FINALIZED_BY)
    private Username finalizedBy;
    @JsonProperty(FINALIZED_DATE)
    private Instant finalizedDate;

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

    public static <T extends TicketEntry> TicketEntry requestNewTicket(Publication publication, Class<T> ticketType) {
        if (DoiRequest.class.equals(ticketType)) {
            return attempt(() -> requestDoiRequestTicket(publication)).orElseThrow();
        } else if (PublishingRequestCase.class.equals(ticketType)) {
            return createOpeningCaseObject(publication);
        } else if (GeneralSupportRequest.class.equals(ticketType)) {
            return GeneralSupportRequest.fromPublication(publication);
        }
        throw new RuntimeException("Unrecognized ticket type");
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

    public Username getFinalizedBy() {
        return finalizedBy;
    }

    public Instant getFinalizedDate() {
        return finalizedDate;
    }

    public void setFinalizedBy(Username finalizedBy) {
        this.finalizedBy = finalizedBy;
    }

    public void setFinalizedDate(Instant finalizedDate) {
        this.finalizedDate = finalizedDate;
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

    public TicketEntry complete(Publication publication, Username finalizedBy) {
        var updated = this.copy();
        updated.setStatus(TicketStatus.COMPLETED);
        updated.validateCompletionRequirements(publication);
        updated.setModifiedDate(Instant.now());
        updated.setFinalizedBy(finalizedBy);
        updated.setFinalizedDate(Instant.now());
        return updated;
    }

    public final TicketEntry close(Username finalizedBy) throws ApiGatewayException {
        validateClosingRequirements();
        var updated = this.copy();
        updated.setStatus(TicketStatus.CLOSED);
        updated.setModifiedDate(Instant.now());
        updated.setFinalizedBy(finalizedBy);
        updated.setFinalizedDate(Instant.now());
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

    public abstract Username getAssignee();

    public abstract void setAssignee(Username assignee);

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
        return ticketService.createTicket(this);
    }

    public final TicketEntry markUnreadByOwner() {
        viewedBy.remove(this.getOwner());
        return this;
    }

    public final TicketEntry markUnreadForEveryone() {
        viewedBy.clear();
        return this;
    }

    public final TicketEntry markReadByUser(User user) {
        viewedBy.add(user);
        return this;
    }

    public TicketEntry fetch(TicketService ticketService) throws NotFoundException {
        return ticketService.fetchTicket(this);
    }

    public TicketEntry markReadByOwner() {
        viewedBy.add(getOwner());
        return this;
    }

    public TicketEntry markReadForAssignee() {
        viewedBy.add(new User(getAssignee().toString()));
        return this;
    }

    public TicketEntry markUnReadForAssignee() {
        viewedBy.remove(new User(getAssignee().toString()));
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

    private static TicketEntry requestDoiRequestTicket(Publication publication) throws BadRequestException {
        if (isPublished(publication)) {
            return DoiRequest.fromPublication(publication);
        } else {
            throw new BadRequestException(DOI_REQUEST_EXCEPTION_MESSAGE_WHEN_NON_PUBLISHED);
        }
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

    public static void setServiceControlledFields(TicketEntry ticketEntry,
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

    private static boolean isPublished(Publication publication) {
        return PUBLISHED_STATUSES.contains(publication.getStatus());
    }

    public abstract void validateAssigneeRequirements(Publication publication);

    public TicketEntry updateAssignee(Publication publication, Username assignee) {
        var updated = this.copy();
        updated.validateAssigneeRequirements(publication);
        updated.setAssignee(assignee);
        updated.setModifiedDate(Instant.now());
        return updated;
    }

    public static final class Constants {

        public static final String STATUS_FIELD = "status";
        public static final String MODIFIED_DATE_FIELD = "modifiedDate";
        public static final String CREATED_DATE_FIELD = "createdDate";
        public static final String OWNER_FIELD = "owner";
        public static final String CUSTOMER_ID_FIELD = "customerId";
        public static final String PUBLICATION_DETAILS_FIELD = "publicationDetails";
        public static final String IDENTIFIER_FIELD = "identifier";
        public static final String WORKFLOW = "workflow";
        public static final String ASSIGNEE_FIELD = "assignee";

        private Constants() {

        }
    }
}
