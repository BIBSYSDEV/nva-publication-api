package no.unit.nva.publication.model.business;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.ASSIGNEE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CREATED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CUSTOMER_ID_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.IDENTIFIER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.MODIFIED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.OWNER_AFFILIATION_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.OWNER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.STATUS_FIELD;
import static nva.commons.core.attempt.Try.attempt;
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
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;

@SuppressWarnings({"PMD.GodClass", "PMD.FinalizeOverloaded", "PMD.ExcessivePublicCount"})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(name = DoiRequest.TYPE, value = DoiRequest.class),
    @JsonSubTypes.Type(name = PublishingRequestCase.TYPE, value = PublishingRequestCase.class),
    @JsonSubTypes.Type(name = FilesApprovalThesis.TYPE, value = FilesApprovalThesis.class),
    @JsonSubTypes.Type(name = GeneralSupportRequest.TYPE, value = GeneralSupportRequest.class),
    @JsonSubTypes.Type(name = UnpublishRequest.TYPE, value = UnpublishRequest.class)})
public abstract class TicketEntry implements Entity {

    public static final String TICKET_WITHOUT_REFERENCE_TO_PUBLICATION_ERROR = "Ticket without reference to "
                                                                               + "publication";
    private static final String VIEWED_BY_FIELD = "viewedBy";
    private static final String FINALIZED_BY = "finalizedBy";
    private static final String FINALIZED_DATE = "finalizedDate";
    private static final String RESOURCE_IDENTIFIER = "resourceIdentifier";
    public static final String REMOVE_NON_PENDING_TICKET_MESSAGE =
        "Cannot remove a ticket that has any other status than %s";
    protected static final String RESPONSIBILITY_AREA_FIELD = "responsibilityArea";
    private static final String RECEIVING_ORGANIZATION_DETAILS = "receivingOrganizationDetails";

    @JsonProperty(IDENTIFIER_FIELD)
    private SortableIdentifier identifier;
    @JsonProperty(STATUS_FIELD)
    private TicketStatus status;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private Instant modifiedDate;
    @JsonProperty(CREATED_DATE_FIELD)
    private Instant createdDate;
    @JsonProperty(OWNER_FIELD)
    private User owner;
    @JsonProperty(VIEWED_BY_FIELD)
    private ViewedBy viewedBy;
    @JsonProperty(ASSIGNEE_FIELD)
    private Username assignee;
    @JsonProperty(RESOURCE_IDENTIFIER)
    private SortableIdentifier resourceIdentifier;
    @JsonProperty(CUSTOMER_ID_FIELD)
    private URI customerId;
    @JsonProperty(FINALIZED_BY)
    private Username finalizedBy;
    @JsonProperty(FINALIZED_DATE)
    private Instant finalizedDate;
    @JsonProperty(OWNER_AFFILIATION_FIELD)
    private URI ownerAffiliation;
    @JsonProperty(RESPONSIBILITY_AREA_FIELD)
    private URI responsibilityArea;
    @JsonProperty(RECEIVING_ORGANIZATION_DETAILS)
    private ReceivingOrganizationDetails receivingOrganizationDetails;

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
        var resource = Resource.fromPublication(publication);
        var userInstance  = UserInstance.fromPublication(publication);
        if (DoiRequest.class.equals(ticketType)) {
            return DoiRequest.create(resource, userInstance);
        } else if (PublishingRequestCase.class.equals(ticketType)) {
            return PublishingRequestCase.create(resource, userInstance, null);
        } else if (GeneralSupportRequest.class.equals(ticketType)) {
            return GeneralSupportRequest.create(resource, userInstance);
        } else if (UnpublishRequest.class.equals(ticketType)) {
            return UnpublishRequest.fromPublication(publication);
        } else if (FilesApprovalThesis.class.equals(ticketType)) {
            return FilesApprovalThesis.createForUserInstitution(resource, userInstance, null);
        }
        throw new RuntimeException("Unrecognized ticket type");
    }

    public static <T extends TicketEntry> T createQueryObject(URI customerId, SortableIdentifier resourceIdentifier,
                                                              Class<T> ticketType) {
        if (DoiRequest.class.equals(ticketType)) {
            return ticketType.cast(
                DoiRequest.builder().withResourceIdentifier(resourceIdentifier).withCustomerId(customerId).build());
        } else if (PublishingRequestCase.class.equals(ticketType)) {
            return ticketType.cast(PublishingRequestCase.createQueryObject(resourceIdentifier, customerId));
        } else if (GeneralSupportRequest.class.equals(ticketType)) {
            return ticketType.cast(GeneralSupportRequest.createQueryObject(customerId, resourceIdentifier));
        } else if (UnpublishRequest.class.equals(ticketType)) {
            return ticketType.cast(UnpublishRequest.createQueryObject(customerId, resourceIdentifier));
        } else if (FilesApprovalThesis.class.equals(ticketType)) {
            return ticketType.cast(FilesApprovalThesis.createQueryObject(customerId, resourceIdentifier));
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static UntypedTicketQueryObject createQueryObject(UserInstance userInstance,
                                                             SortableIdentifier ticketIdentifier) {
        return UntypedTicketQueryObject.create(userInstance, ticketIdentifier);
    }

    public static UntypedTicketQueryObject createQueryObject(SortableIdentifier ticketIdentifier) {
        return UntypedTicketQueryObject.create(ticketIdentifier);
    }

    public static TicketEntry createNewUnpublishRequest(Publication publication,
                                                        Supplier<SortableIdentifier> identifierProvider) {
        var ticket = UnpublishRequest.fromPublication(publication);
        setServiceControlledFields(ticket, identifierProvider);
        return ticket;
    }

    public static void setServiceControlledFields(TicketEntry ticketEntry,
                                                  Supplier<SortableIdentifier> identifierProvider) {
        var now = Instant.now();
        ticketEntry.setCreatedDate(now);
        ticketEntry.setModifiedDate(now);
        ticketEntry.setIdentifier(identifierProvider.get());
        if (nonNull(ticketEntry.getFinalizedDate())) {
            ticketEntry.setFinalizedDate(now);
        }
    }

    @Override
    public Publication toPublication(ResourceService resourceService) {
        return attempt(() -> resourceService.getPublicationByIdentifier(getResourceIdentifier())).orElseThrow();
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    public void setResourceIdentifier(SortableIdentifier resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    @Override
    public URI getCustomerId() {
        return customerId;
    }

    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }

    @Override
    public void setCreatedDate(Instant now) {
        this.createdDate = now;
    }

    @Override
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @Override
    public void setModifiedDate(Instant now) {
        this.modifiedDate = now;
    }

    public Username getAssignee() {
        return assignee;
    }

    public void setAssignee(Username assignee) {
        this.assignee = assignee;
    }

    @Override
    public String getStatusString() {
        return status.toString();
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Username getFinalizedBy() {
        return finalizedBy;
    }

    public void setFinalizedBy(Username finalizedBy) {
        this.finalizedBy = finalizedBy;
    }

    public Instant getFinalizedDate() {
        return finalizedDate;
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

    @Override
    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public URI getOwnerAffiliation() {
        return ownerAffiliation;
    }

    public void setOwnerAffiliation(URI ownerAffiliation) {
        this.ownerAffiliation = ownerAffiliation;
    }

    public ReceivingOrganizationDetails getReceivingOrganizationDetails() {
        return receivingOrganizationDetails;
    }

    public void setReceivingOrganizationDetails(ReceivingOrganizationDetails receivingOrganizationDetails) {
        this.receivingOrganizationDetails = receivingOrganizationDetails;
    }

    public TicketEntry updateReceivingOrganizationDetails(URI ownerAffiliation, URI responsibilityArea) {
        setReceivingOrganizationDetails(new ReceivingOrganizationDetails(ownerAffiliation, responsibilityArea));
        return this;
    }

    public void persistUpdate(TicketService ticketService) {
        ticketService.updateTicket(this);
    }

    public abstract void validateCreationRequirements(Publication publication) throws ConflictException;

    public abstract void validateCompletionRequirements(Publication publication);

    public TicketEntry complete(Publication publication, UserInstance userInstance) {
        var updated = this.copy();
        var now = Instant.now();
        updated.setModifiedDate(now);
        updated.setFinalizedDate(now);
        updated.setFinalizedBy(new Username(userInstance.getUser().toString()));
        updated.setStatus(TicketStatus.COMPLETED);
        updated.validateCompletionRequirements(publication);
        updated.setViewedBy(ViewedBy.addAll(userInstance.getUser()));
        return updated;
    }

    public TicketEntry close(UserInstance userInstance) throws ApiGatewayException {
        validateClosingRequirements();
        var updated = this.copy();
        updated.setStatus(TicketStatus.CLOSED);
        updated.setModifiedDate(Instant.now());
        updated.setFinalizedBy(new Username(userInstance.getUsername()));
        updated.setFinalizedDate(Instant.now());
        updated.setViewedBy(ViewedBy.addAll(userInstance.getUser()));
        return updated;
    }

    public final TicketEntry remove(UserInstance userInstance) throws ApiGatewayException {
        validateRemovingRequirements(userInstance);
        var updated = this.copy();
        updated.setStatus(TicketStatus.REMOVED);
        var now = Instant.now();
        updated.setModifiedDate(now);
        updated.setFinalizedDate(now);
        return updated;
    }

    public void validateClosingRequirements() throws ApiGatewayException {
        if (!getStatus().equals(TicketStatus.PENDING)) {
            var errorMessage = String.format("Cannot close a ticket that has any other status than %s",
                                             TicketStatus.PENDING);
            throw new BadRequestException(errorMessage);
        }
    }

    public void validateRemovingRequirements(UserInstance userInstance) throws ApiGatewayException {
        validateTicketForRemovalStatusRequirement();
        validateTicketOwner(userInstance);
    }

    public boolean isPending() {
        return TicketStatus.PENDING.equals(getStatus());
    }

    public TicketEntry withOwner(String username) {
        this.owner = new User(username);
        return this;
    }

    public URI getResponsibilityArea() {
        return responsibilityArea;
    }

    public void setResponsibilityArea(URI responsibilityArea) {
        this.responsibilityArea = responsibilityArea;
    }

    private void validateTicketOwner(UserInstance userInstance) throws ForbiddenException {
        if (isNotTicketOwner(userInstance)) {
            throw new ForbiddenException();
        }
    }

    private void validateTicketForRemovalStatusRequirement() throws BadRequestException {
        if (isNotPending()) {
            var errorMessage = String.format(REMOVE_NON_PENDING_TICKET_MESSAGE, TicketStatus.PENDING);
            throw new BadRequestException(errorMessage);
        }
    }

    private boolean isNotPending() {
        return !TicketStatus.PENDING.equals(getStatus());
    }

    private boolean isNotTicketOwner(UserInstance userInstance) {
        return !userInstance.getUser().equals(getOwner());
    }

    public abstract TicketEntry copy();

    @Override
    public abstract TicketDao toDao();

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

    public TicketEntry updateAssignee(Publication publication, Username assignee) {
        var updated = this.copy();
        updated.setAssignee(assignee);
        updated.setModifiedDate(Instant.now());
        return updated;
    }

    public TicketEntry withOwnerAffiliation(URI ownerAffiliation) {
        this.setOwnerAffiliation(ownerAffiliation);
        return this;
    }

    public boolean hasSameOwnerAffiliationAs(UserInstance userInstance) {
        return Optional.ofNullable(this.getOwnerAffiliation())
                   .map(value -> value.equals(userInstance.getTopLevelOrgCristinId()))
                   .orElse(false);
    }

    protected static ReceivingOrganizationDetails createDefaultReceivingOrganizationDetails(UserInstance userInstance) {
        return new ReceivingOrganizationDetails(userInstance.getTopLevelOrgCristinId(),
                                                userInstance.getPersonAffiliation());
    }

    private static <T extends TicketEntry> TicketEntry createNewTicketEntry(
        Publication publication,
        Class<T> ticketType,
        Supplier<SortableIdentifier> identifierProvider) {

        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication);
        if (DoiRequest.class.equals(ticketType)) {
            return DoiRequest.create(resource, userInstance);
        } else if (PublishingRequestCase.class.equals(ticketType)) {
            return PublishingRequestCase.create(resource, userInstance, null);
        } else if (GeneralSupportRequest.class.equals(ticketType)) {
            return GeneralSupportRequest.create(resource, userInstance);
        } else if (UnpublishRequest.class.equals(ticketType)) {
            return createNewUnpublishRequest(publication, identifierProvider);
        } else if (FilesApprovalThesis.class.equals(ticketType)) {
            return FilesApprovalThesis.createForUserInstitution(resource, userInstance, null);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public boolean hasAssignee() {
        return nonNull(this.getAssignee());
    }

    public static final class Constants {

        public static final String STATUS_FIELD = "status";
        public static final String MODIFIED_DATE_FIELD = "modifiedDate";
        public static final String CREATED_DATE_FIELD = "createdDate";
        public static final String OWNER_FIELD = "owner";
        public static final String CUSTOMER_ID_FIELD = "customerId";
        public static final String RESOURCE_IDENTIFIER_FIELD = "resourceIdentifier";
        public static final String IDENTIFIER_FIELD = "identifier";
        public static final String WORKFLOW = "workflow";
        public static final String ASSIGNEE_FIELD = "assignee";
        public static final String OWNER_AFFILIATION_FIELD = "ownerAffiliation";
        public static final String APPROVED_FILES_FIELD = "approvedFiles";
        public static final String FILES_FOR_APPROVAL_FIELD = "filesForApproval";

        private Constants() {

        }
    }
}
