package no.unit.nva.publication.publishingrequest.list;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("PublishingRequest")
public class PublishingRequestDto {

    public static final String IDENTIFIER = "identifier";
    public static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    public static final String STATUS = "status";

    @JsonProperty(IDENTIFIER)
    private final SortableIdentifier identifier;
    @JsonProperty(PUBLICATION_IDENTIFIER)
    private final SortableIdentifier publicationIdentifier;
    @JsonProperty(STATUS)
    private final PublishingRequestStatus status;

    public PublishingRequestDto(@JsonProperty(IDENTIFIER) SortableIdentifier identifier,
                                @JsonProperty(PUBLICATION_IDENTIFIER) SortableIdentifier publicationIdentifier,
                                @JsonProperty(STATUS) PublishingRequestStatus status) {
        this.identifier = identifier;
        this.publicationIdentifier = publicationIdentifier;
        this.status = status;
    }

    public static PublishingRequestDto fromPublishingRequest(PublishingRequest publishingRequest) {
        return new PublishingRequestDto(publishingRequest.getIdentifier(),
                                        publishingRequest.getResourceIdentifier(),
                                        publishingRequest.getStatus());
    }

    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    public SortableIdentifier getPublicationIdentifier() {
        return publicationIdentifier;
    }

    public PublishingRequestStatus getStatus() {
        return status;
    }
}
