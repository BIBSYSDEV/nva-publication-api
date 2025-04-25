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
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
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
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(DoiRequestDto.class), @JsonSubTypes.Type(PublishingRequestDto.class),
    @JsonSubTypes.Type(GeneralSupportRequestDto.class), @JsonSubTypes.Type(UnpublishRequestDto.class),
    @JsonSubTypes.Type(FileApprovalThesisDto.class)})
public abstract class TicketDto implements JsonSerializable {

    public static final String STATUS_FIELD = "status";
    public static final String MESSAGES_FIELD = "messages";
    public static final String VIEWED_BY = "viewedBy";
    public static final String ASSIGNEE_FIELD = "assignee";
    public static final String OWNER_FIELD = "owner";
    public static final String OWNER_AFFILIATION_FIELD = "ownerAffiliation";
    public static final String PUBLICATION_IDENTIFIER_FIELD = "publicationIdentifier";
    public static final String FINALIZED_BY_FIELD = "finalizedBy";
    public static final String FINALIZED_DATE_FIELD = "finalizedDate";
    public static final String CREATED_DATE_FIELD = "createdDate";
    public static final String MODIFIED_DATE_FIELD = "modifiedDate";
    public static final String IDENTIFIER_FIELD = "identifier";
    public static final String ID_FIELD = "id";

    @JsonProperty(ID_FIELD)
    private final URI id;
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

    @SuppressWarnings({"PMD.ExcessiveParameterList"})
    protected TicketDto(URI id, SortableIdentifier identifier, TicketDtoStatus status, List<MessageDto> messages,
                        Set<User> viewedBy, Username assignee, SortableIdentifier publicationIdentifier, User owner,
                        URI ownerAffiliation, Username finalizedBy, Instant finalizedDate, Instant createdDate,
                        Instant modifiedDate) {
        this.id = id;
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
    }

    public static TicketDto fromTicket(TicketEntry ticket) {
        return fromTicket(ticket, Collections.emptyList());
    }

    public static TicketDto fromTicket(TicketEntry ticket, Collection<Message> messages) {
        return create(ticket, messages);
    }

    public static TicketDto create(TicketEntry ticket, Collection<Message> messages) {
        var messageDtos = messages.stream().map(MessageDto::fromMessage).collect(Collectors.toList());
        return TicketDto.builder()
                   .withCreatedDate(ticket.getCreatedDate())
                   .withStatus(getTicketDtoStatus(ticket))
                   .withModifiedDate(ticket.getModifiedDate())
                   .withIdentifier(ticket.getIdentifier())
                   .withId(createTicketId(ticket))
                   .withPublicationIdentifier(ticket.getResourceIdentifier())
                   .withMessages(messageDtos)
                   .withViewedBy(ticket.getViewedBy())
                   .withAssignee(ticket.getAssignee())
                   .withOwnerAffiliation(ticket.getOwnerAffiliation())
                   .withOwner(ticket.getOwner())
                   .withFinalizedBy(ticket.getFinalizedBy())
                   .withFinalizedDate(ticket.getFinalizedDate())
                   .build(ticket);
    }

    public static Builder builder() {
        return new TicketDto.Builder();
    }

    public static URI createTicketId(TicketEntry ticket) {
        return UriWrapper.fromUri(createPublicationId(ticket.getResourceIdentifier()))
                   .addChild(PublicationServiceConfig.TICKET_PATH)
                   .addChild(ticket.getIdentifier().toString())
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

    public URI getId() {
        return id;
    }

    private static URI createPublicationId(SortableIdentifier publicationIdentifier) {
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
        private URI id;
        private List<MessageDto> messages;
        private ViewedBy viewedBy;
        private SortableIdentifier publicationIdentifier;
        private Username assignee;
        private User owner;
        private URI ownerAffiliation;
        private Username finalizedBy;
        private Instant finalizedDate;

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

        public Builder withId(URI id) {
            this.id = id;
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
            return new UnpublishRequestDto(status, createdDate, modifiedDate, identifier, publicationIdentifier, id,
                                           messages, viewedBy, assignee, owner, ownerAffiliation, finalizedBy,
                                           finalizedDate);
        }

        private GeneralSupportRequestDto createGeneralSupportCaseDto() {
            return new GeneralSupportRequestDto(status, createdDate, modifiedDate, identifier,
                                                publicationIdentifier, id, messages, viewedBy, assignee, owner,
                                                ownerAffiliation, finalizedBy, finalizedDate);
        }

        public Builder withViewedBy(Set<User> viewedBy) {
            this.viewedBy = new ViewedBy(viewedBy);
            return this;
        }

        private PublishingRequestDto createPublishingRequestDto(PublishingRequestCase publishingRequestCase) {
            return new PublishingRequestDto(status, createdDate, modifiedDate, identifier, publicationIdentifier, id,
                                            messages, viewedBy, assignee, owner, ownerAffiliation,
                                            publishingRequestCase.getWorkflow(),
                                            publishingRequestCase.getApprovedFiles(),
                                            publishingRequestCase.getFilesForApproval(),
                                            finalizedBy, finalizedDate);
        }

        private FileApprovalThesisDto createFilesApprovalThesisDto(FilesApprovalThesis filesApprovalThesis) {
            return new FileApprovalThesisDto(status, createdDate, modifiedDate, identifier, publicationIdentifier, id,
                                            messages, viewedBy, assignee, owner, ownerAffiliation,
                                            filesApprovalThesis.getWorkflow(), filesApprovalThesis.getApprovedFiles(),
                                             filesApprovalThesis.getFilesForApproval(),
                                            finalizedBy, finalizedDate);
        }

        private DoiRequestDto createDoiRequestDto() {
            return new DoiRequestDto(status, createdDate, modifiedDate, identifier, publicationIdentifier, id, messages,
                                     viewedBy, assignee, owner, ownerAffiliation, finalizedBy, finalizedDate);
        }
    }
}
