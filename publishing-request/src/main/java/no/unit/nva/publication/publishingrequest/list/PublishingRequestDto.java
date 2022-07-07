package no.unit.nva.publication.publishingrequest.list;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import nva.commons.core.JacocoGenerated;

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

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublishingRequestDto)) {
            return false;
        }
        PublishingRequestDto that = (PublishingRequestDto) o;
        return Objects.equals(getIdentifier(), that.getIdentifier()) && Objects.equals(
            getPublicationIdentifier(), that.getPublicationIdentifier()) && getStatus() == that.getStatus();
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getPublicationIdentifier(), getStatus());
    }
}
