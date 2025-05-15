package no.unit.nva.publication.ticket;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(UnpublishRequestDto.TYPE)
public class UnpublishRequestDto extends TicketDto {
    public static final String TYPE = "UnpublishRequest";


    @SuppressWarnings({"PMD.ExcessiveParameterList"})
    @JsonCreator
    public UnpublishRequestDto(@JsonProperty(STATUS_FIELD) TicketDtoStatus status,
                               @JsonProperty(CREATED_DATE_FIELD) Instant createdDate,
                               @JsonProperty(MODIFIED_DATE_FIELD) Instant modifiedDate,
                               @JsonProperty(IDENTIFIER_FIELD) SortableIdentifier identifier,
                               @JsonProperty(PUBLICATION_IDENTIFIER_FIELD) SortableIdentifier publicationIdentifier,
                               @JsonProperty(MESSAGES_FIELD) List<MessageDto> messages,
                               @JsonProperty(VIEWED_BY) Set<User> viewedBy,
                               @JsonProperty(ASSIGNEE_FIELD) Username assignee,
                               @JsonProperty(OWNER_FIELD) User owner,
                               @JsonProperty(OWNER_AFFILIATION_FIELD) URI ownerAffiliation,
                               @JsonProperty(FINALIZED_BY_FIELD) Username finalizedBy,
                               @JsonProperty(FINALIZED_DATE_FIELD) Instant finalizedDate,
                               @JsonProperty(AVAILABLE_INSTITUTIONS_FIELD) Collection<URI> availableInstitutions) {
        super(identifier,
              status,
              messages,
              viewedBy,
              assignee,
              publicationIdentifier,
              owner,
              ownerAffiliation,
              finalizedBy,
              finalizedDate,
              createdDate,
              modifiedDate,
              availableInstitutions);

    }

    @JacocoGenerated
    @Override
    public Class<? extends TicketEntry> ticketType() {
        return UnpublishRequest.class;
    }
}
