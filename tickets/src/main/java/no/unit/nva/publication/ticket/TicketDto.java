package no.unit.nva.publication.ticket;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.ticket.utils.TicketDtoStatusMapper.getTicketDtoStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.TicketOperation;
import no.unit.nva.model.Username;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.ViewedBy;
import no.unit.nva.publication.permissions.ticket.TicketPermissions;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings({"PMD.ExcessiveParameterList", "PMD.CouplingBetweenObjects"})
@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(DoiRequestDto.class), @JsonSubTypes.Type(PublishingRequestDto.class),
    @JsonSubTypes.Type(GeneralSupportRequestDto.class), @JsonSubTypes.Type(UnpublishRequestDto.class),
    @JsonSubTypes.Type(FilesApprovalThesisDto.class)})
public abstract class TicketDto implements JsonSerializable {

    protected static final String STATUS_FIELD = "status";
    protected static final String MESSAGES_FIELD = "messages";
    protected static final String VIEWED_BY = "viewedBy";
    protected static final String ASSIGNEE_FIELD = "assignee";
    protected static final String OWNER_FIELD = "owner";
    protected static final String OWNER_AFFILIATION_FIELD = "ownerAffiliation";
    protected static final String PUBLICATION_IDENTIFIER_FIELD = "publicationIdentifier";
    protected static final String FINALIZED_BY_FIELD = "finalizedBy";
    protected static final String FINALIZED_DATE_FIELD = "finalizedDate";
    protected static final String CREATED_DATE_FIELD = "createdDate";
    protected static final String MODIFIED_DATE_FIELD = "modifiedDate";
    protected static final String IDENTIFIER_FIELD = "identifier";
    protected static final String ID_FIELD = "id";
    protected static final String AVAILABLE_INSTITUTIONS_FIELD = "availableInstitutions";
    public static final String ALLOWED_OPERATIONS_FIELD = "allowedOperations";

    @JsonProperty(IDENTIFIER_FIELD)
    private final SortableIdentifier identifier;
    @JsonProperty(CREATED_DATE_FIELD)
    private final Instant createdDate;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private final Instant modifiedDate;
    @JsonProperty(STATUS_FIELD)
    private final TicketDtoStatus status;
    @JsonProperty(VIEWED_BY)
    private final Set<User> viewedBy;
    @JsonProperty(MESSAGES_FIELD)
    private final List<MessageDto> messages;
    @JsonProperty(ASSIGNEE_FIELD)
    private final Username assignee;
    @JsonProperty(PUBLICATION_IDENTIFIER_FIELD)
    private final SortableIdentifier publicationIdentifier;
    @JsonProperty(OWNER_FIELD)
    private final User owner;
    @JsonProperty(OWNER_AFFILIATION_FIELD)
    private final URI ownerAffiliation;
    @JsonProperty(FINALIZED_BY_FIELD)
    private final Username finalizedBy;
    @JsonProperty(FINALIZED_DATE_FIELD)
    private final Instant finalizedDate;
    @JsonProperty(AVAILABLE_INSTITUTIONS_FIELD)
    private final Set<URI> availableInstitutions;
    @JsonProperty(ALLOWED_OPERATIONS_FIELD)
    protected Set<TicketOperation> allowedOperations;

    protected TicketDto(SortableIdentifier identifier, TicketDtoStatus status, List<MessageDto> messages,
                        Set<User> viewedBy, Username assignee, SortableIdentifier publicationIdentifier, User owner,
                        URI ownerAffiliation, Username finalizedBy, Instant finalizedDate, Instant createdDate,
                        Instant modifiedDate, Collection<URI> availableInstitutions,
                        Set<TicketOperation> allowedOperations) {
        this.identifier = identifier;
        this.status = status;
        this.messages = messages;
        this.viewedBy = new ViewedBy(viewedBy);
        this.assignee = assignee;
        this.publicationIdentifier = publicationIdentifier;
        this.owner = owner;
        this.ownerAffiliation = ownerAffiliation;
        this.finalizedBy = finalizedBy;
        this.finalizedDate = finalizedDate;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.availableInstitutions = nonNull(availableInstitutions) ? Set.copyOf(availableInstitutions) : Set.of();
        this.allowedOperations = nonNull(allowedOperations) ? Set.copyOf(allowedOperations) : Set.of();
    }

    public static TicketDto fromTicket(TicketEntry ticket, Collection<Message> messages,
                                       Collection<URI> availableInstitutions, TicketPermissions ticketPermissions) {
        var messageDtos = messages.stream().map(MessageDto::fromMessage).collect(Collectors.toList());
        return TicketDto.builder()
                   .withCreatedDate(ticket.getCreatedDate())
                   .withStatus(getTicketDtoStatus(ticket))
                   .withModifiedDate(ticket.getModifiedDate())
                   .withIdentifier(ticket.getIdentifier())
                   .withPublicationIdentifier(ticket.getResourceIdentifier())
                   .withMessages(messageDtos)
                   .withViewedBy(ticket.getViewedBy())
                   .withAssignee(ticket.getAssignee())
                   .withOwnerAffiliation(ticket.getOwnerAffiliation())
                   .withOwner(ticket.getOwner())
                   .withFinalizedBy(ticket.getFinalizedBy())
                   .withFinalizedDate(ticket.getFinalizedDate())
                   .withAvailableInstitutions(availableInstitutions)
                   .withAllowedOperations(ticketPermissions.getAllAllowedActions())
                   .build(ticket);
    }

    public static Builder builder() {
        return new TicketDto.Builder();
    }

    public static URI createTicketId(SortableIdentifier resourceIdentifier, SortableIdentifier ticketIdentifier) {
        return Optional.ofNullable(resourceIdentifier)
                   .map(TicketDto::createPublicationId)
                   .map(resourceId -> toTicketIdentifier(resourceId, ticketIdentifier))
                   .orElse(null);
    }

    public Set<TicketOperation> getAllowedOperations() {
        return allowedOperations;
    }

    private static URI toTicketIdentifier(URI resourceId, SortableIdentifier ticketIdentifier) {
        return UriWrapper.fromUri(resourceId)
                   .addChild(PublicationServiceConfig.TICKET_PATH)
                   .addChild(ticketIdentifier.toString())
                   .getUri();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getId(), getIdentifier(), getCreatedDate(), getModifiedDate(), getStatus(), getViewedBy(),
                            getMessages(), getAssignee(), getPublicationIdentifier(), getOwner(), getOwnerAffiliation(),
                            getFinalizedBy(), getFinalizedDate());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TicketDto ticketDto)) {
            return false;
        }
        return Objects.equals(getId(), ticketDto.getId()) &&
               Objects.equals(getIdentifier(), ticketDto.getIdentifier()) &&
               Objects.equals(getCreatedDate(), ticketDto.getCreatedDate()) &&
               Objects.equals(getModifiedDate(), ticketDto.getModifiedDate()) && getStatus() == ticketDto.getStatus() &&
               Objects.equals(getViewedBy(), ticketDto.getViewedBy()) &&
               Objects.equals(getMessages(), ticketDto.getMessages()) &&
               Objects.equals(getAssignee(), ticketDto.getAssignee()) &&
               Objects.equals(getPublicationIdentifier(), ticketDto.getPublicationIdentifier()) &&
               Objects.equals(getOwner(), ticketDto.getOwner()) &&
               Objects.equals(getOwnerAffiliation(), ticketDto.getOwnerAffiliation()) &&
               Objects.equals(getFinalizedBy(), ticketDto.getFinalizedBy()) &&
               Objects.equals(getFinalizedDate(), ticketDto.getFinalizedDate());
    }

    public SortableIdentifier getPublicationIdentifier() {
        return publicationIdentifier;
    }

    public Username getAssignee() {
        return assignee;
    }

    public User getOwner() {
        return owner;
    }

    public URI getOwnerAffiliation() {
        return ownerAffiliation;
    }

    public abstract Class<? extends TicketEntry> ticketType();

    public final TicketDtoStatus getStatus() {
        return status;
    }

    public final List<MessageDto> getMessages() {
        return nonNull(messages) ? messages : Collections.emptyList();
    }

    public Set<User> getViewedBy() {
        return viewedBy;
    }

    public Username getFinalizedBy() {
        return finalizedBy;
    }

    public Instant getFinalizedDate() {
        return finalizedDate;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @JsonProperty(ID_FIELD)
    public URI getId() {
        return createTicketId(publicationIdentifier, identifier);
    }

    public Set<URI> getAvailableInstitutions() {
        return availableInstitutions;
    }

    private static URI createPublicationId(SortableIdentifier publicationIdentifier) {
        return Optional.ofNullable(publicationIdentifier)
                   .map(TicketDto::toPublicationId)
                   .orElse(null);
    }

    private static URI toPublicationId(SortableIdentifier publicationIdentifier) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PublicationServiceConfig.PUBLICATION_PATH)
                   .addChild(publicationIdentifier.toString())
                   .getUri();
    }

    public static final class Builder {

        private TicketDtoStatus status;
        private Instant createdDate;
        private Instant modifiedDate;
        private SortableIdentifier identifier;
        private List<MessageDto> messages;
        private ViewedBy viewedBy;
        private SortableIdentifier publicationIdentifier;
        private Username assignee;
        private User owner;
        private URI ownerAffiliation;
        private Username finalizedBy;
        private Instant finalizedDate;
        private Collection<URI> availableInstitutions;
        private Set<TicketOperation> allowedOperations;

        private Builder() {
        }

        public Builder withStatus(TicketDtoStatus status) {
            this.status = status;
            return this;
        }

        public Builder withCreatedDate(Instant createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            this.modifiedDate = modifiedDate;
            return this;
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withMessages(List<MessageDto> messages) {
            this.messages = messages;
            return this;
        }

        public Builder withAssignee(Username assignee) {
            this.assignee = assignee;
            return this;
        }

        public Builder withOwner(User owner) {
            this.owner = owner;
            return this;
        }

        public Builder withOwnerAffiliation(URI ownerAffiliation) {
            this.ownerAffiliation = ownerAffiliation;
            return this;
        }

        public Builder withPublicationIdentifier(SortableIdentifier publicationIdentifier) {
            this.publicationIdentifier = publicationIdentifier;
            return this;
        }

        public Builder withFinalizedBy(Username finalizedBy) {
            this.finalizedBy = finalizedBy;
            return this;
        }

        public Builder withFinalizedDate(Instant finalizedDate) {
            this.finalizedDate = finalizedDate;
            return this;
        }

        public Builder withAvailableInstitutions(Collection<URI> availableInstitutions) {
            this.availableInstitutions = availableInstitutions;
            return this;
        }

        public Builder withAllowedOperations(Set<TicketOperation> allowedOperations) {
            this.allowedOperations = allowedOperations;
            return this;
        }

        public TicketDto build(TicketEntry ticketEntry) {
            return switch (ticketEntry) {
                case DoiRequest ignored -> createDoiRequestDto();
                case PublishingRequestCase publishingRequestCase -> createPublishingRequestDto(publishingRequestCase);
                case GeneralSupportRequest ignored -> createGeneralSupportCaseDto();
                case UnpublishRequest ignored -> createUnpublishRequestDto();
                case FilesApprovalThesis filesApprovalThesis -> createFilesApprovalThesisDto(filesApprovalThesis);
                default -> throw new RuntimeException("Unsupported type");
            };
        }

        private UnpublishRequestDto createUnpublishRequestDto() {
            return new UnpublishRequestDto(status, createdDate, modifiedDate, identifier, publicationIdentifier,
                                           messages, viewedBy, assignee, owner, ownerAffiliation, finalizedBy,
                                           finalizedDate, availableInstitutions, allowedOperations);
        }

        private GeneralSupportRequestDto createGeneralSupportCaseDto() {
            return new GeneralSupportRequestDto(status, createdDate, modifiedDate, identifier,
                                                publicationIdentifier, messages, viewedBy, assignee, owner,
                                                ownerAffiliation, finalizedBy, finalizedDate, availableInstitutions,
                                                allowedOperations);
        }

        public Builder withViewedBy(Set<User> viewedBy) {
            this.viewedBy = new ViewedBy(viewedBy);
            return this;
        }

        private PublishingRequestDto createPublishingRequestDto(PublishingRequestCase publishingRequestCase) {
            return new PublishingRequestDto(status, createdDate, modifiedDate, identifier, publicationIdentifier,
                                            messages, viewedBy, assignee, owner, ownerAffiliation,
                                            publishingRequestCase.getWorkflow(),
                                            publishingRequestCase.getApprovedFiles(),
                                            publishingRequestCase.getFilesForApproval(),
                                            finalizedBy, finalizedDate, availableInstitutions, allowedOperations);
        }

        private FilesApprovalThesisDto createFilesApprovalThesisDto(FilesApprovalThesis filesApprovalThesis) {
            return new FilesApprovalThesisDto(status, createdDate, modifiedDate, identifier, publicationIdentifier,
                                              messages, viewedBy, assignee, owner, ownerAffiliation,
                                              filesApprovalThesis.getWorkflow(), filesApprovalThesis.getApprovedFiles(),
                                              filesApprovalThesis.getFilesForApproval(),
                                              finalizedBy, finalizedDate, availableInstitutions, allowedOperations);
        }

        private DoiRequestDto createDoiRequestDto() {
            return new DoiRequestDto(status, createdDate, modifiedDate, identifier, publicationIdentifier, messages,
                                     viewedBy, assignee, owner, ownerAffiliation, finalizedBy, finalizedDate,
                                     availableInstitutions, allowedOperations);
        }
    }
}
